package hardwar.branch.prediction.judged.SAs;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC;
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PSPHT; // per set predication history table
    private final HashMode hashMode;

    public SAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public SAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;
        this.hashMode = hashMode;

        // Initialize the PSBHR with the given bhr and branch instruction size
        PSBHR = new RegisterBank(KSize, BHRSize);

        // Initializing the PSPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PSPHT = new PerAddressPredictionHistoryTable(KSize, (1<<BHRSize), SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC", SCSize, null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        Bit[] hashaddr = CombinationalLogic.hash(branchInstruction.getInstructionAddress(), KSize, hashMode);
        ShiftRegister BHR = PSBHR.read(hashaddr);
        Bit[] current = BHR.read();
        //System.err.println(PHT.monitor());
        Bit[] entry = getCacheEntry(branchInstruction.getInstructionAddress(), current);
        PSPHT.setDefault(entry, getDefaultBlock());
        Bit[] values = PSPHT.get(entry);
        SC.load(values);
        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] currentNum = SC.read();
        if(actual.equals(BranchResult.TAKEN)) {
            currentNum = CombinationalLogic.count(currentNum, true, CountMode.SATURATING);
        }
        else {
            currentNum = CombinationalLogic.count(currentNum, false, CountMode.SATURATING);
        }
        Bit[] hashaddr = CombinationalLogic.hash(branchInstruction.getInstructionAddress(), KSize, hashMode);
        ShiftRegister BHR = PSBHR.read(hashaddr);
        Bit[] current = BHR.read();
        PSPHT.put(getCacheEntry(branchInstruction.getInstructionAddress(), current), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            BHR.insert(Bit.ONE);
        }
        else {
            BHR.insert(Bit.ZERO);
        }
        PSBHR.write(hashaddr, BHR.read());
    }


    private Bit[] getAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return CombinationalLogic.hash(branchAddress, KSize, hashMode);
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, KSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return null;
    }
}
