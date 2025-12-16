package io.github.dimkich.integration.testing.initialization;

import java.util.ArrayList;
import java.util.List;

/**
 * A stack-like data structure with cursor functionality for tracking and reading
 * elements added since the last read operation. This class combines stack semantics
 * with the ability to mark positions in the element sequence and retrieve elements
 * added after those positions.
 *
 * <p>The cursor starts at position 0 (beginning of the list). When {@link #readFromCursor()}
 * is called, it returns all elements from the current cursor position to the end,
 * then moves the cursor to the end. This is useful for scenarios where you need to
 * process only new elements added since the last read.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * CursorStack<String> stack = new CursorStack<>();
 * stack.push("A");
 * stack.push("B");
 * stack.readFromCursor(); // Returns ["A", "B"], cursor moves to end
 * stack.push("C");
 * stack.readFromCursor(); // Returns ["C"], cursor moves to end
 * stack.resetCursor(); // Cursor returns to position 0
 * stack.readFromCursor(); // Returns ["A", "B", "C"], cursor moves to end
 * }
 * </pre>
 *
 * @param <T> the type of elements stored in this stack
 */
public class CursorStack<T> {
    private final List<T> elements = new ArrayList<>();
    private int cursor = 0;

    /**
     * Adds an element to the top of the stack (end of the internal list).
     * The cursor position remains unchanged unless it was beyond the previous
     * size (in which case it's adjusted by {@link #pop()}).
     *
     * @param item the element to be added to the stack. Cannot be {@code null}
     *             if the implementation doesn't explicitly support null values
     *             (this implementation uses {@link ArrayList} which allows nulls).
     */
    public void push(T item) {
        elements.add(item);
    }

    /**
     * Removes and returns the element from the top of the stack (end of the list).
     * If the cursor was positioned beyond the new size after removal, it's adjusted
     * to point to the new end of the list (the last element).
     *
     * @return the element that was removed from the top of the stack
     * @throws IndexOutOfBoundsException if the stack is empty
     */
    public T pop() {
        T removed = elements.remove(elements.size() - 1);
        if (cursor > elements.size()) {
            cursor = elements.size();
        }
        return removed;
    }

    /**
     * Returns a list containing all elements from the current cursor position
     * to the end of the stack. After this operation, the cursor is moved to
     * the end of the stack (position equal to stack size).
     *
     * <p>If the cursor is at or beyond the end of the stack, an empty list is
     * returned and the cursor remains unchanged (still points to end).
     *
     * <p>The returned list is a view of a portion of the internal list, so
     * modifications to the returned list will affect the internal storage.
     * For a snapshot that won't be affected by subsequent modifications,
     * create a copy of the returned list.
     *
     * @return a list of elements from the cursor position to the end of the stack.
     * Returns an empty list if cursor is at or beyond the stack end.
     */
    public List<T> readFromCursor() {
        if (cursor >= elements.size()) {
            return List.of();
        }
        List<T> result = elements.subList(cursor, elements.size());
        cursor = elements.size();
        return result;
    }

    /**
     * Resets the cursor to the beginning of the stack (position 0).
     * Subsequent calls to {@link #readFromCursor()} will read from the
     * first element again.
     */
    public void resetCursor() {
        cursor = 0;
    }

    /**
     * Checks whether there are any elements available to read from the
     * current cursor position. This returns {@code true} if the cursor
     * is positioned before at least one element (cursor < size).
     *
     * <p>Note: This method's name might be slightly confusing as it doesn't
     * check if the entire stack is empty, but rather if there are elements
     * from the cursor position onward. An empty stack will always return
     * {@code false} from this method.
     *
     * @return {@code true} if there are elements from the cursor position
     * to the end of the stack, {@code false} otherwise
     */
    public boolean isEmpty() {
        return cursor < elements.size();
    }
}