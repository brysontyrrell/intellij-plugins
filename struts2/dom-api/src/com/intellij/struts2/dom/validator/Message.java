/*
 * Copyright 2011 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.dom.validator;

import com.intellij.struts2.dom.StrutsDomConstants;
import com.intellij.util.xml.*;

/**
 * <code>message</code>.
 *
 * @author Yann C&eacute;bron
 */
@Namespace(StrutsDomConstants.VALIDATOR_NAMESPACE_KEY)
@Presentation(typeName = "Message", icon = "/resources/icons/message.png")
public interface Message extends DomElement {

  GenericAttributeValue<String> getKey();

  @TagValue
  String getValue();

}