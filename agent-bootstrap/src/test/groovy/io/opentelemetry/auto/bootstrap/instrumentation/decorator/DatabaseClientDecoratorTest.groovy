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

package io.opentelemetry.auto.bootstrap.instrumentation.decorator

import io.opentelemetry.trace.Span
import io.opentelemetry.trace.attributes.SemanticAttributes

class DatabaseClientDecoratorTest extends ClientDecoratorTest {

  def "test afterStart"() {
    setup:
    def decorator = newDecorator()

    when:
    decorator.afterStart(span)

    then:
    1 * span.setAttribute(SemanticAttributes.DB_TYPE.key(), "test-db")
    0 * _

    where:
    serviceName << ["test-service", "other-service", null]
  }

  def "test onConnection"() {
    setup:
    def decorator = newDecorator()

    when:
    decorator.onConnection(span, session)

    then:
    if (session) {
      1 * span.setAttribute(SemanticAttributes.DB_USER.key(), session.user)
      1 * span.setAttribute(SemanticAttributes.DB_INSTANCE.key(), session.instance)
      1 * span.setAttribute(SemanticAttributes.DB_URL.key(), session.url)
    }
    0 * _

    where:
    session << [
      null,
      [user: "test-user"],
      [instance: "test-instance", url: "test:"],
      [user: "test-user", instance: "test-instance"]
    ]
  }

  def "test onStatement"() {
    setup:
    def decorator = newDecorator()

    when:
    decorator.onStatement(span, statement)

    then:
    1 * span.setAttribute(SemanticAttributes.DB_STATEMENT.key(), statement)
    0 * _

    where:
    statement      | _
    null           | _
    ""             | _
    "db-statement" | _
  }

  def "test assert null span"() {
    setup:
    def decorator = newDecorator()

    when:
    decorator.afterStart((Span) null)

    then:
    thrown(AssertionError)

    when:
    decorator.onConnection((Span) null, null)

    then:
    thrown(AssertionError)

    when:
    decorator.onStatement((Span) null, null)

    then:
    thrown(AssertionError)
  }

  @Override
  def newDecorator() {
    return new DatabaseClientDecorator<Map>() {

      @Override
      protected String dbType() {
        return "test-db"
      }

      @Override
      protected String dbUser(Map map) {
        return map.user
      }

      @Override
      protected String dbInstance(Map map) {
        return map.instance
      }

      @Override
      protected String dbUrl(Map map) {
        return map.url
      }
    }
  }
}
