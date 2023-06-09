package hardwar.branch.prediction.judged.SAg;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAg implements BranchPredictor {
    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public SAg() {
        this(4, 2, 8, 4);
    }

    public SAg(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;

        // Initialize the PABHR with the given bhr and Ksize
        PSBHR = new RegisterBank(KSize ,BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable((1<<KSize), SCSize);

        // Initialize the SC register
        Bit[] zero = new Bit[SCSize];
        for(int i=0;i<SCSize;i++) {
            zero[i] = Bit.ZERO;
        }
        SC = new SIPORegister("SC", SCSize, zero);
    }

    @Override
    public BranchResult predict(BranchInstruction instruction) {
        Bit[] hashaddr = hash(instruction.getInstructionAddress());
        ShiftRegister BHR = PSBHR.read(hashaddr);
        Bit[] current = BHR.read();
        PHT.setDefault(current, getDefaultBlock());
        Bit[] values = PHT.get(current);
        SC.load(values);

        if(values[0] == Bit.ONE) {
            return BranchResult.TAKEN;
        } else {
            return BranchResult.NOT_TAKEN;
        }
        // TODO: complete Task 1
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
        Bit[] hashaddr = hash(branchInstruction.getInstructionAddress());
        ShiftRegister BHR = PSBHR.read(hashaddr);
        Bit[] current = BHR.read();
        PHT.put(BHR.read(), currentNum);
        if(actual.equals(BranchResult.TAKEN)) {
            BHR.insert(Bit.ONE);
        }
        else {
            BHR.insert(Bit.ZERO);
        }
        PSBHR.write(hashaddr, BHR.read());
    }

    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return hash(branchAddress);
    }

    /**
     * hash N bits to a K bit value
     *
     * @param bits program counter
     * @return hash value of fist M bits of `bits` in K bits
     */
    private Bit[] hash(Bit[] bits) {
        Bit[] hash = new Bit[KSize];

        // XOR the first M bits of the PC to produce the hash
        for (int i = 0; i < branchInstructionSize; i++) {
            int j = i % KSize;
            if (hash[j] == null) {
                hash[j] = bits[i];
            } else {
                Bit xorProduce = hash[j].getValue() ^ bits[i].getValue() ? Bit.ONE : Bit.ZERO;
                hash[j] = xorProduce;

            }
        }
        return hash;
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
