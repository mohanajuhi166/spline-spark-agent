/*
 * Copyright 2020 ABSA Group Limited
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

package za.co.absa.spline.harvester.logging

import org.mockito.Mockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.harvester.logging.ObjectStructureDumper.ExtractFieldValueFn

class ObjectStructureDumperSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "dump()"

  it should "handle cycles" in {
    case class Foo(var bar: Any)

    val foo1 = Foo("hi")
    val foo2 = Foo(foo1)
    foo1.bar = foo2

    ObjectStructureDumper.dump(foo1) should include("bar: java.lang.Object ! Object was already logged")
  }

  it should "survive reflection errors" in {
    case class Foo(bar: String, baz: String)

    val foo = Foo("bar", "baz")

    val extractFieldValueFnMock1: ExtractFieldValueFn = mock[ExtractFieldValueFn]
    val extractFieldValueFnMock2: ExtractFieldValueFn = mock[ExtractFieldValueFn]
    Mockito.when(extractFieldValueFnMock1.apply(foo, "bar")).thenThrow(new RuntimeException("fake"))
    Mockito.when(extractFieldValueFnMock2.apply(foo, "baz")).thenThrow(new RuntimeException("fake"))

    (ObjectStructureDumper.dump(foo, extractFieldValueFnMock1)
      should (include("bar: java.lang.String = ! error occurred: fake at za.co.absa.spline.harvester.logging.ObjectStructureDumper")
      and include("baz: java.lang.String = null")
      ))
    (ObjectStructureDumper.dump(foo, extractFieldValueFnMock2)
      should (include("bar: java.lang.String = null")
      and include("baz: java.lang.String = ! error occurred: fake at za.co.absa.spline.harvester.logging.ObjectStructureDumper")
      ))
  }
}
