package hardwar.branch.prediction.judged.GAp;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAp implements BranchPredictor {
    private final int branchInstructionSize;
    private final ShiftRegister SC; // saturating counter register
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PAPHT; // Per Address History Table

    public GAp() {
        this(4, 2, 8);
    }

    /**
     * Creates a new GAp predictor with the given BHR register size and initializes the PAPHT based on
     * the branch instruction length and saturating counter size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public GAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the BHR register with the given size and no default value
        this.BHR = new SIPORegister("BHR", BHRSize, null);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PAPHT = new PerAddressPredictionHistoryTable(branchInstructionSize, BHRSize, SCSize);

        // Initialize the SC register
        Bit[] zero = new Bit[SCSize];
        for(int i=0;i<SCSize;i++) {
            zero[i] = Bit.ZERO;
        }
        SC = new SIPORegister("SC", SCSize, zero);
    }

    /**
     * predicts the result of a branch instruction based on the global branch history and branch address
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO: complete Task 1
        Bit[] current = BHR.read();
        PAPHT.setDefault(getCacheEntry(branchInstruction.getInstructionAddress()), getDefaultBlock());
        Bit[] values = PAPHT.get(getCacheEntry(branchInstruction.getInstructionAddress()));
        
        SC.load(values);


        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
    }

    /**
     * Updates the value in the cache based on actual branch result
     *
     * @param branchInstruction the branch instruction
     * @param actual            the actual result of branch (Taken or Not)
     */
    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO : complete Task 2
        Bit[] currentNum = SC.read();
        if(actual.equals(BranchResult.TAKEN)) {
            currentNum = CombinationalLogic.count(currentNum, true, CountMode.SATURATING);
        }
        else {
            currentNum = CombinationalLogic.count(currentNum, false, CountMode.SATURATING);
        }

        //PHT.put(BHR.read(), currentNum);
        PAPHT.put(getCacheEntry(branchInstruction.getInstructionAddress()), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            BHR.insert(Bit.ONE);
        }
        else {
            BHR.insert(Bit.ZERO);
        }
    }


    /**
     * concat the branch address and BHR to retrieve the desired address
     *
     * @param branchAddress program counter
     * @return concatenated value of first M bits of branch address and BHR
     */
    private Bit[] getCacheEntry(Bit[] branchAddress) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] bhrBits = BHR.read();
        Bit[] cacheEntry = new Bit[branchAddress.length + bhrBits.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(bhrBits, 0, cacheEntry, branchAddress.length, bhrBits.length);
        return cacheEntry;
    }

    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    /**
     * @return snapshot of caches and registers content
     */
    @Override
    public String monitor() {
        return "GAp predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PAPHT.monitor();
    }

}

