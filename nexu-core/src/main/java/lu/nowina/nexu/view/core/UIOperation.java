/**
 * © Nowina Solutions, 2015-2015
 *
 * Licensed under the European Union Public Licence (EUPL).
 */
package lu.nowina.nexu.view.core;

import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lu.nowina.nexu.api.flow.AbstractFutureOperationInvocation;
import lu.nowina.nexu.api.flow.BasicOperationStatus;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.api.flow.OperationStatus;
import lu.nowina.nexu.flow.Flow;
import lu.nowina.nexu.flow.operation.UIDisplayAwareOperation;

/**
 * Headless description of a user interaction requested by a {@link Flow}.
 *
 * <p>The core stores a view identifier and controller parameters, but it
 * never loads JavaFX classes. A {@link UIDisplay} implementation supplied
 * by the desktop application renders the operation and signals completion.</p>
 *
 * @param <R> operation result type
 */
public class UIOperation<R> implements UIDisplayAwareOperation<R> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIOperation.class);

    private final transient Object lock = new Object();
    private transient volatile OperationResult<R> result;

    private UIDisplay display;
    private String viewResource;
    private Object[] controllerParams = new Object[0];

    public UIOperation() {
        // Required by the operation factory.
    }

    public void setParams(final Object... params) {
        if (params == null || params.length < 1) {
            throw new IllegalArgumentException("A UIOperation needs a view resource.");
        }
        try {
            viewResource = (String) params[0];
            if (params.length > 1 && params[1] instanceof Object[]) {
                final Object[] nestedParams = (Object[]) params[1];
                controllerParams = Arrays.copyOf(nestedParams, nestedParams.length);
            } else if (params.length > 1) {
                controllerParams = Arrays.copyOfRange(params, 1, params.length);
            } else {
                controllerParams = new Object[0];
            }
        } catch (ClassCastException exception) {
            throw new IllegalArgumentException(
                    "Expected parameters: view resource, controller parameters.",
                    exception);
        }
    }

    public final String getViewResource() {
        return viewResource;
    }

    public final Object[] getControllerParams() {
        return Arrays.copyOf(controllerParams, controllerParams.length);
    }

    @Override
    public final OperationResult<R> perform() {
        LOGGER.info("Displaying {}", viewResource);
        display();
        return result;
    }

    public final void waitEnd() throws InterruptedException {
        final String operationName = operationName();
        LOGGER.info("Thread {} waits on {}", Thread.currentThread().getName(), operationName);
        synchronized (lock) {
            while (result == null) {
                lock.wait();
            }
        }
        LOGGER.info("Thread {} resumed on {}", Thread.currentThread().getName(), operationName);
    }

    public final void signalEnd(final R value) {
        notifyResult(new OperationResult<>(value));
        hide();
    }

    public final void signalEnd(final OperationStatus operationStatus) {
        notifyResult(new OperationResult<>(operationStatus));
    }

    public final void signalUserCancel() {
        notifyResult(new OperationResult<>(BasicOperationStatus.USER_CANCEL));
    }

    private void notifyResult(final OperationResult<R> operationResult) {
        result = operationResult;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private String operationName() {
        return viewResource == null ? getClass().getSimpleName() : viewResource;
    }

    @Override
    public final void setDisplay(final UIDisplay display) {
        this.display = Objects.requireNonNull(display, "display");
    }

    protected final UIDisplay getDisplay() {
        return display;
    }

    protected void display() {
        display.displayAndWaitUIOperation(this);
    }

    protected void hide() {
        display.close(true);
    }

    @Override
    public int hashCode() {
        int value = 17;
        value = 31 * value + Objects.hashCode(display);
        value = 31 * value + Objects.hashCode(viewResource);
        value = 31 * value + Arrays.hashCode(controllerParams);
        return value;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }
        final UIOperation<?> other = (UIOperation<?>) otherObject;
        return Objects.equals(display, other.display)
                && Objects.equals(viewResource, other.viewResource)
                && Arrays.equals(controllerParams, other.controllerParams);
    }

    public static <R, T extends UIOperation<R>> FutureOperationInvocation<R>
            getFutureOperationInvocation(
                    final Class<T> operationClass,
                    final String viewResource,
                    final Object... controllerParams) {
        return new UIFutureOperationInvocation<>(
                operationClass,
                viewResource,
                controllerParams);
    }

    private static final class UIFutureOperationInvocation<R, T extends UIOperation<R>>
            extends AbstractFutureOperationInvocation<R> {

        private final Class<T> operationClass;
        private final String viewResource;
        private final Object[] controllerParams;

        private UIFutureOperationInvocation(
                final Class<T> operationClass,
                final String viewResource,
                final Object... controllerParams) {
            this.operationClass = operationClass;
            this.viewResource = viewResource;
            this.controllerParams = controllerParams == null
                    ? new Object[0]
                    : Arrays.copyOf(controllerParams, controllerParams.length);
        }

        @Override
        protected Class<T> getOperationClass() {
            return operationClass;
        }

        @Override
        protected Object[] getOperationParams() {
            return new Object[] {
                viewResource,
                Arrays.copyOf(controllerParams, controllerParams.length)
            };
        }
    }
}
