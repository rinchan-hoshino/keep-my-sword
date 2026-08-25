package dev.rinchan.keepmysword.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

public final class BrokenTintRenderState {
    private final ThreadLocal<Deque<Boolean>> decisions = new ThreadLocal<>();

    public void enter(BooleanSupplier computation) {
        Deque<Boolean> stack = decisions.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            decisions.set(stack);
        }
        stack.push(computation.getAsBoolean());
    }

    public boolean shouldTint() {
        Deque<Boolean> stack = decisions.get();
        return stack != null && !stack.isEmpty() && stack.peek();
    }

    public void exit() {
        Deque<Boolean> stack = decisions.get();
        if (stack == null) {
            return;
        }
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            decisions.remove();
        }
    }
}
