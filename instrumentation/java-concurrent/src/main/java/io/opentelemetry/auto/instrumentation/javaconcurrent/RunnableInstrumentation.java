/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.instrumentation.javaconcurrent;

import static io.opentelemetry.auto.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.bootstrap.ContextStore;
import io.opentelemetry.auto.bootstrap.InstrumentationContext;
import io.opentelemetry.auto.bootstrap.instrumentation.java.concurrent.AdviceUtils;
import io.opentelemetry.auto.bootstrap.instrumentation.java.concurrent.State;
import io.opentelemetry.auto.instrumentation.api.SpanWithScope;
import io.opentelemetry.auto.tooling.Instrumenter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/** Instrument {@link Runnable} and {@Callable} */
@Slf4j
@AutoService(Instrumenter.class)
public final class RunnableInstrumentation extends Instrumenter.Default {

  public RunnableInstrumentation() {
    super(AbstractExecutorInstrumentation.EXEC_NAME);
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named(Runnable.class.getName()));
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap(Runnable.class.getName(), State.class.getName());
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("run").and(takesArguments(0)).and(isPublic()),
        RunnableInstrumentation.class.getName() + "$RunnableAdvice");
  }

  public static class RunnableAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanWithScope enter(@Advice.This final Runnable thiz) {
      final ContextStore<Runnable, State> contextStore =
          InstrumentationContext.get(Runnable.class, State.class);
      return AdviceUtils.startTaskScope(contextStore, thiz);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(@Advice.Enter final SpanWithScope scope) {
      AdviceUtils.endTaskScope(scope);
    }
  }
}
