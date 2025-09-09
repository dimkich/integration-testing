package io.github.dimkich.integration.testing.execution;

import lombok.Data;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;

@Data
public class ConstructorMockAnswer implements Answer<Object> {
    private final Answer<Object> answer;
    private Object object;

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        return answer.answer(new InvocationOnMock() {
            @Override
            public Object getMock() {
                return invocation.getMock();
            }

            @Override
            public Method getMethod() {
                return invocation.getMethod();
            }

            @Override
            public Object[] getRawArguments() {
                return invocation.getRawArguments();
            }

            @Override
            public Object[] getArguments() {
                return invocation.getArguments();
            }

            @Override
            public <T> T getArgument(int index) {
                return invocation.getArgument(index);
            }

            @Override
            public <T> T getArgument(int index, Class<T> clazz) {
                return invocation.getArgument(index, clazz);
            }

            @Override
            public Object callRealMethod() throws Throwable {
                return invocation.getMethod().invoke(object, invocation.getRawArguments());
            }
        });
    }
}
