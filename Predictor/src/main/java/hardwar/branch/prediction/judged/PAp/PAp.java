package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize,BHRSize);;

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

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        ShiftRegister current = PABHR.read(branchInstruction.getInstructionAddress());
        Bit[] pht = getCacheEntry(branchInstruction.getInstructionAddress(),current.read() );
        PAPHT.setDefault(pht, getDefaultBlock());
        Bit[] values = PAPHT.get(pht);
        SC.load(values);

        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
        // TODO: complete Task 1
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        ShiftRegister current = PABHR.read(instruction.getInstructionAddress());
        Bit[] currentNum = SC.read();
        if(actual.equals(BranchResult.TAKEN)) {
            currentNum = CombinationalLogic.count(currentNum, true, CountMode.SATURATING);
        }
        else {
            currentNum = CombinationalLogic.count(currentNum, false, CountMode.SATURATING);
        }
        PAPHT.put(current.read(), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            current.insert(Bit.ONE);
            PABHR.write(instruction.getInstructionAddress(),current.read());
        }
        else {
            current.insert(Bit.ZERO);
            PABHR.write(instruction.getInstructionAddress(),current.read());
        }
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
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
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
