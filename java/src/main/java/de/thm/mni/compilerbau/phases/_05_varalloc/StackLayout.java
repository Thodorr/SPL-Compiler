package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.utils.NotImplemented;

/**
 * This class describes the stack frame layout of a procedure.
 * It contains the sizes of the various subareas and provides methods to retrieve information about the stack frame required to generate code for the procedure.
 */
public class StackLayout {
    // The following values have to be set in phase 5
    public Integer argumentAreaSize = null;
    public Integer localVarAreaSize = null;
    public Integer outgoingAreaSize = null;
    public boolean isOptimizedLeafProcedure = false;  // Only relevant for --leafProc

    /**
     * @return The total size of the stack frame described by this object.
     */
    public int frameSize() {
        final int frameSize = Math.max(outgoingAreaSize, 0) + (outgoingAreaSize != -1 ? 4 : 0) + 4 + localVarAreaSize;
        return frameSize;
    }

    /**
     * @return The offset (starting from the new stack pointer) where the old frame pointer is stored in this stack frame.
     */
    public int oldFramePointerOffset() {
        final int oldFramePtrOffset = Math.max(outgoingAreaSize, 0) + (outgoingAreaSize != -1 ? 4 : 0);
        return oldFramePtrOffset;
    }

    /**
     * @return The offset (starting from the new frame pointer) where the old return address is stored in this stack frame.
     */
    public int oldReturnAddressOffset() {
        final int oldReturnPtrOffset = -4 - 4 - localVarAreaSize;
        return oldReturnPtrOffset;
    }
}
