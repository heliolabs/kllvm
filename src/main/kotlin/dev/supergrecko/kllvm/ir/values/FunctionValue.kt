package dev.supergrecko.kllvm.ir.values

import dev.supergrecko.kllvm.internal.contracts.Unreachable
import dev.supergrecko.kllvm.internal.util.map
import dev.supergrecko.kllvm.internal.util.toBoolean
import dev.supergrecko.kllvm.ir.Attribute
import dev.supergrecko.kllvm.ir.AttributeIndex
import dev.supergrecko.kllvm.ir.BasicBlock
import dev.supergrecko.kllvm.ir.CallConvention
import dev.supergrecko.kllvm.ir.Value
import dev.supergrecko.kllvm.support.VerifierFailureAction
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMAttributeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

public class FunctionValue internal constructor() : Value() {
    /**
     * Construct a new Type from an LLVM pointer reference
     */
    public constructor(llvmValue: LLVMValueRef) : this() {
        ref = llvmValue
    }

    //region Core::BasicBlock
    public fun appendBasicBlock(name: String): BasicBlock {
        return BasicBlock(LLVM.LLVMAppendBasicBlock(ref, name))
    }
    //endregion Core::BasicBlock

    //region Core::Values::Constants::FunctionValues::FunctionParameters
    /**
     * Get a parameter from this function at [index]
     *
     * @see LLVM.LLVMGetParam
     */
    public fun getParameter(index: Int): Value {
        require(index < getParameterCount())

        val value = LLVM.LLVMGetParam(ref, index)

        return Value(value)
    }

    /**
     * Get the amount of parameters this function expects
     *
     * @see LLVM.LLVMCountParams
     */
    public fun getParameterCount(): Int {
        return LLVM.LLVMCountParams(ref)
    }

    /**
     * Get all parameters from this function
     *
     * @see LLVM.LLVMGetParams
     */
    public fun getParameters(): List<Value> {
         val ptr = PointerPointer<LLVMValueRef>(getParameterCount().toLong())

        LLVM.LLVMGetParams(ref, ptr)

        return ptr.map { Value(it) }
    }

    /**
     * Set the parameter alignment for parameter [value]
     */
    public fun setParameterAlignment(value: Value, align: Int) {
        LLVM.LLVMSetParamAlignment(value.ref, align)
    }
    //endregion Core::Values::Constants::FunctionValues::FunctionParameters

    //region Core::Values::Constants::FunctionValues
    /**
     * Set the call convention for this function
     *
     * @see LLVM.LLVMGetFunctionCallConv
     * @see LLVM.LLVMSetFunctionCallConv
     */
    public var callConvention: CallConvention
        get() {
            val cc = LLVM.LLVMGetFunctionCallConv(ref)

            return CallConvention.values()
                .firstOrNull { it.value == cc }
                ?: throw Unreachable()
        }
        set(value) {
            LLVM.LLVMSetFunctionCallConv(ref, value.value)
        }

    /**
     * Set the personality function for this function
     *
     * @see LLVM.LLVMGetPersonalityFn
     * @see LLVM.LLVMSetPersonalityFn
     */
    public var personalityFunction: FunctionValue
        get() {
            require(hasPersonalityFunction())

            val fn = LLVM.LLVMGetPersonalityFn(ref)

            return FunctionValue(fn)
        }
        set(value) {
            LLVM.LLVMSetPersonalityFn(ref, value.ref)
        }

    /**
     * Set the garbage collector name for this function
     *
     * @see LLVM.LLVMGetGC
     * @see LLVM.LLVMSetGC
     */
    public var garbageCollector: String
        get() = LLVM.LLVMGetGC(ref).string
        set(value) = LLVM.LLVMSetGC(ref, value)

    /**
     * Determine if this function has a personality function
     *
     * @see LLVM.LLVMHasPersonalityFn
     */
    public fun hasPersonalityFunction(): Boolean {
        return LLVM.LLVMHasPersonalityFn(ref).toBoolean()
    }

    /**
     * Delete this function from its parent
     *
     * @see LLVM.LLVMDeleteFunction
     */
    public fun delete() {
        LLVM.LLVMDeleteFunction(ref)
    }

    /**
     * If this function is an intrinsic, get its id
     *
     * @see LLVM.LLVMGetIntrinsicID
     */
    public fun getIntrinsicId(): Int {
        return LLVM.LLVMGetIntrinsicID(ref)
    }

    /**
     * Add an attribute at an [index]
     *
     * @see LLVM.LLVMAddAttributeAtIndex
     */
    public fun addAttribute(index: AttributeIndex, attribute: Attribute) {
        addAttribute(index.value.toInt(), attribute)
    }

    /**
     * Add an attribute at an [index]
     *
     * @see LLVM.LLVMAddAttributeAtIndex
     */
    public fun addAttribute(index: Int, attribute: Attribute) {
        LLVM.LLVMAddAttributeAtIndex(ref, index, attribute.ref)
    }

    /**
     * Get the amount of attributes at an [index]
     *
     * @see LLVM.LLVMGetAttributeCountAtIndex
     */
    public fun getAttributeCount(index: AttributeIndex): Int {
        return LLVM.LLVMGetAttributeCountAtIndex(ref, index.value.toInt())
    }

    /**
     * Get all attributes at an [index] for the function
     *
     * @see LLVM.LLVMGetAttributesAtIndex
     */
    public fun getAttributes(index: AttributeIndex): List<Attribute> {
        val ptr = PointerPointer<LLVMAttributeRef>(
            getAttributeCount(index).toLong()
        )

        LLVM.LLVMGetAttributesAtIndex(ref, index.value.toInt(), ptr)

        return ptr.map { Attribute(it) }
    }

    /**
     * Pull the attribute value from an [index] with a [kind]
     *
     * @see LLVM.LLVMGetEnumAttributeAtIndex
     */
    public fun getAttribute(
        index: AttributeIndex,
        kind: Int
    ): Attribute {
        val ref = LLVM.LLVMGetEnumAttributeAtIndex(
            ref, index.value.toInt(), kind
        )

        return Attribute(ref)
    }

    /**
     * Pull the attribute value from an [index] with a [kind]
     *
     * @see LLVM.LLVMGetStringAttributeAtIndex
     */
    public fun getAttribute(
        index: AttributeIndex,
        kind: String
    ): Attribute {
        val ref = LLVM.LLVMGetStringAttributeAtIndex(
            ref, index.value.toInt(), kind, kind.length
        )

        return Attribute(ref)
    }

    /**
     * Removes an attribute at the given index
     *
     * @see LLVM.LLVMRemoveEnumAttributeAtIndex
     */
    public fun removeAttribute(
        index: AttributeIndex,
        kind: Int
    ) {
        LLVM.LLVMRemoveEnumAttributeAtIndex(
            ref, index.value.toInt(), kind
        )
    }

    /**
     * Removes an attribute at the given index
     *
     * @see LLVM.LLVMRemoveStringAttributeAtIndex
     */
    public fun removeAttribute(
        index: AttributeIndex,
        kind: String
    ) {
        LLVM.LLVMRemoveStringAttributeAtIndex(
            ref, index.value.toInt(), kind, kind.length
        )
    }

    /**
     * @see LLVM.LLVMAddTargetDependentFunctionAttr
     */
    public fun addTargetDependentAttribute(
        attribute: String,
        value: String
    ) {
        LLVM.LLVMAddTargetDependentFunctionAttr(ref, attribute, value)
    }
    //endregion Core::Values::Constants::FunctionValues

    //region Analysis
    /**
     * Verify that the function structure is valid
     *
     * As opposed to the LLVM implementation, this returns true if the function
     * is valid.
     */
    public fun verify(action: VerifierFailureAction): Boolean {
        // LLVM Source says:
        // > Note that this function's return value is inverted from what you would
        // > expect of a function called "verify".
        // Thus we invert it again ...
        return !LLVM.LLVMVerifyFunction(ref, action.value).toBoolean()
    }

    /**
     * View the function structure
     *
     * From the LLVM Source:
     *
     * This function is meant for use from the debugger. You can just say
     * 'call F->viewCFG()' and a ghost view window should pop up from the
     * program, displaying the CFG of the current function. This depends on
     * there being a 'dot' and 'gv' program in your path.
     *
     * If [hideBasicBlocks] is true then [LLVM.LLVMViewFunctionCFGOnly] will be
     * used instead of [LLVM.LLVMViewFunctionCFG]
     *
     * TODO: Does this even work via JNI??
     */
    public fun viewConfiguration(hideBasicBlocks: Boolean) {
        if (hideBasicBlocks) {
            LLVM.LLVMViewFunctionCFGOnly(ref)
        } else {
            LLVM.LLVMViewFunctionCFG(ref)
        }
    }
    //endregion Analysis
}