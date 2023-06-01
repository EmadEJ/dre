package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {
        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
        Bit[] init = new Bit[BHRSize];
        for(int i=0;i<SCSize;i++) {
            init[i] = Bit.ZERO;
        }
        this.BHR = new SIPORegister("BHR", BHRSize, init);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable((1<<BHRSize), SCSize);

        // Initialize the SC register
        Bit[] zero = new Bit[SCSize];
        for(int i=0;i<SCSize;i++) {
            zero[i] = Bit.ZERO;
        }
        SC = new SIPORegister("SC", SCSize, zero);
    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        // TODO : complete Task 1
        Bit[] current = BHR.read();
        System.err.println(current);
        PHT.setDefault(current, getDefaultBlock());
        Bit[] values = PHT.get(current);
        SC.load(values);

        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] currentNum = SC.read();
        if(actual.equals(BranchResult.TAKEN)) {
            CombinationalLogic.count(currentNum, true, CountMode.SATURATING);
        }
        else {
            CombinationalLogic.count(currentNum, false, CountMode.SATURATING);
        }
        PHT.put(BHR.read(), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            BHR.insert(Bit.ONE);
        }
        else {
            BHR.insert(Bit.ZERO);
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
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
