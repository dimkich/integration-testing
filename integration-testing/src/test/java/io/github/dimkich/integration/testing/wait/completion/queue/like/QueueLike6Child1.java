package io.github.dimkich.integration.testing.wait.completion.queue.like;

public class QueueLike6Child1 extends QueueLike6 {
    @Override
    @QueueLike6Ann
    public QueueLike6 create() {
        return new QueueLike6Child1();
    }
}
