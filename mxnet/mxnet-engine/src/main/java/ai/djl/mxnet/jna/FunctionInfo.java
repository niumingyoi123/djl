/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.mxnet.jna;

import ai.djl.Device;
import ai.djl.mxnet.engine.MxNDManager;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.training.Trainer;
import ai.djl.util.PairList;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A FunctionInfo represents an operator (ie function) within the MXNet Engine. */
public class FunctionInfo {

    private Pointer handle;
    private String name;
    private PairList<String, String> arguments;

    private static final Logger logger = LoggerFactory.getLogger(Trainer.class);

    FunctionInfo(Pointer pointer, String functionName, PairList<String, String> arguments) {
        this.handle = pointer;
        this.name = functionName;
        this.arguments = arguments;
    }

    /**
     * Calls an operator with the given arguments.
     *
     * @param manager the manager to attach the result to
     * @param src the input NDArray(s) to the operator
     * @param dest the destination NDArray(s) to be overwritten with the result of the operator
     * @param params the non-NDArray arguments to the operator. Should be a {@code PairList<String,
     *     String>}
     * @return the error code or zero for no errors
     */
    public int invoke(
            NDManager manager, NDArray[] src, NDArray[] dest, PairList<String, ?> params) {
        checkDevices(src);
        checkDevices(dest);
        PointerArray srcHandles = JnaUtils.toPointerArray(src);
        PointerByReference destRef = new PointerByReference(JnaUtils.toPointerArray(dest));
        JnaUtils.imperativeInvoke(handle, srcHandles, destRef, params);
        return 0;
    }

    /**
     * Calls an operator with the given arguments.
     *
     * @param manager the manager to attach the result to
     * @param src the input NDArray(s) to the operator
     * @param params the non-NDArray arguments to the operator. Should be a {@code PairList<String,
     *     String>}
     * @return the error code or zero for no errors
     */
    public NDArray[] invoke(NDManager manager, NDArray[] src, PairList<String, ?> params) {
        checkDevices(src);
        PointerArray srcHandles = JnaUtils.toPointerArray(src);
        return invoke((MxNDManager) manager, srcHandles, params);
    }

    /**
     * Calls an operator with the given arguments.
     *
     * @param manager the manager to attach the result to
     * @param src the input NDArray pointers to the operator
     * @param params the non-NDArray arguments to the operator. Should be a {@code PairList<String,
     *     String>}
     * @return the error code or zero for no errors
     */
    private NDArray[] invoke(MxNDManager manager, PointerArray src, PairList<String, ?> params) {
        PointerByReference destRef = new PointerByReference();
        Pointer[] pointers = JnaUtils.imperativeInvoke(handle, src, destRef, params);
        NDArray[] ret = new NDArray[pointers.length];
        for (int i = 0; i < pointers.length; ++i) {
            ret[i] = manager.create(pointers[i]);
        }
        return ret;
    }

    /**
     * Returns the name of the operator.
     *
     * @return the name of the operator
     */
    public String getFunctionName() {
        return name;
    }

    /**
     * Returns the names of the params to the operator.
     *
     * @return the names of the params to the operator
     */
    public List<String> getArgumentNames() {
        return arguments.keys();
    }

    /**
     * Returns the types of the operator arguments.
     *
     * @return the types of the operator arguments
     */
    public List<String> getArgumentTypes() {
        return arguments.values();
    }

    private void checkDevices(NDArray[] src) {
        // check if all the NDArrays are in the same device
        if (logger.isDebugEnabled() && src.length > 1) {
            Device device = src[0].getDevice();
            for (int i = 1; i < src.length; ++i) {
                if (!device.equals(src[i].getDevice())) {
                    logger.warn(
                            "Please make sure all the NDArrays are in the same device. You can call toDevice() to move the NDArray to the desired Device.");
                }
            }
        }
    }
}
