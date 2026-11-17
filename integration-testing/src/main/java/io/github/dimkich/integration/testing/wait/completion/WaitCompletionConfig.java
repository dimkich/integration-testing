package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeWaitCompletion;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({WaitCompletionList.class, FutureLikeWaitCompletion.class, MethodCountingWaitCompletion.class,
        MethodPairWaitCompletion.class, QueueLikeWaitCompletion.class})
public class WaitCompletionConfig {
}
