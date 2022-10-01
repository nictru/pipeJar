package example;

import pipeline.ExecutableStep;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class ThirdStep extends ExecutableStep {
    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                add(() -> {
                    System.out.println(finalI);
                    return true;
                });
            }
        }};
    }

    @Override
    protected boolean mayBeSkipped() {
        return false;
    }
}
