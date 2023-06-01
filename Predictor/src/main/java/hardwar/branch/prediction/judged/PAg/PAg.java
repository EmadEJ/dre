package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public PAg() {
        this(4, 2, 8);
    }

    /**
     * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
     * the branch instruction size and BHR size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public PAg(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        // Initialize the PABHR with the given bhr and branch instruction size

        PABHR = new RegisterBank(branchInstructionSize,BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT  = new PageHistoryTable((1<<BHRSize), SCSize);

        // Initialize the SC register
        Bit[] zero = new Bit[SCSize];
        for(int i=0;i<SCSize;i++) {
            zero[i] = Bit.ZERO;
        }
        SC = new SIPORegister("SC", SCSize, zero);
    }

    /**
     * @param instruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction instruction) {
        ShiftRegister current = PABHR.read(instruction.getInstructionAddress());
        PHT.setDefault(current.read(), getDefaultBlock());
        Bit[] values = PHT.get(current.read());
        SC.load(values);

        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
        // TODO: complete Task 1
    }

    /**
     * @param instruction the branch instruction
     * @param actual      the actual result of branch (taken or not)
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        ShiftRegister current = PABHR.read(instruction.getInstructionAddress());
        // TODO: complete Task 2
        Bit[] currentNum = SC.read();
        if(actual.equals(BranchResult.TAKEN)) {
            currentNum = CombinationalLogic.count(currentNum, true, CountMode.SATURATING);
        }
        else {
            currentNum = CombinationalLogic.count(currentNum, false, CountMode.SATURATING);
        }
        PHT.put(current.read(), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            current.insert(Bit.ONE);
            PABHR.write(instruction.getInstructionAddress(),current.read());
        }
        else {
            current.insert(Bit.ZERO);
            PABHR.write(instruction.getInstructionAddress(),current.read());
        }
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
        return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
