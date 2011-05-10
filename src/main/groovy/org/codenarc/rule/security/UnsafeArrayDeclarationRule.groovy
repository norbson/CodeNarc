/*
 * Copyright 2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.security

import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codehaus.groovy.ast.FieldNode
import java.lang.reflect.Modifier
import org.codenarc.util.AstUtil

/**
 * Triggers a violation when an array is declared public, final, and static. Secure coding principles state that, in most cases, an array declared public, final and static is a bug because arrays are mutable objects.
 *
 * @author 'Hamlet D'Arcy'
 * @version $Revision: 24 $ - $Date: 2009-01-31 13:47:09 +0100 (Sat, 31 Jan 2009) $
 */
class UnsafeArrayDeclarationRule extends AbstractAstVisitorRule {
    String name = 'UnsafeArrayDeclaration'
    int priority = 2
    Class astVisitorClass = UnsafeArrayDeclarationAstVisitor
}

class UnsafeArrayDeclarationAstVisitor extends AbstractAstVisitor {
    @Override
    void visitFieldEx(FieldNode node) {
        if (isPublicStaticFinal(node) && isArray(node)) {
            addViolation(node, "The Array field $node.name is public, static, and final but still mutable")
        }
        super.visitFieldEx(node)
    }

    private static boolean isArray(FieldNode node) {
        AstUtil.getFieldType(node)?.isArray()
    }

    private static boolean isPublicStaticFinal(FieldNode node) {
        Modifier.isFinal(node.modifiers) &&
                Modifier.isStatic(node.modifiers) &&
                Modifier.isPublic(node.modifiers)
    }


}