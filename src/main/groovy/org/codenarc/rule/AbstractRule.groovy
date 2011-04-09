/*
 * Copyright 2008 the original author or authors.
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
package org.codenarc.rule

import org.codehaus.groovy.ast.ImportNode
import org.codenarc.source.SourceCode
import org.codenarc.source.SourceCodeCriteria
import org.apache.log4j.Logger
import org.codehaus.groovy.ast.ASTNode

/**
 * Abstract superclass for Rules.
 * <p/>
 * Each subclass must define an <code>name</code> property (String) and a <code>priority</code> property
 * (integer 1..3).
 *
 * @author Chris Mair
 * @version $Revision$ - $Date$
 */
@SuppressWarnings('DuplicateLiteral')
abstract class AbstractRule implements Rule {
    static final LOG = Logger.getLogger(AbstractRule)

    /**
     * Flag indicating whether this rule should be enabled (applied). Defaults to true.
     * If set to false, this rule will not produce any violations.
     */
    boolean enabled = true

    /**
     * This rule is only applied to source code (file) pathnames matching this regular expression.
     */
    String applyToFilesMatching

    /**
     * This rule is NOT applied to source code (file) pathnames matching this regular expression.
     */
    String doNotApplyToFilesMatching

    /**
     * This rule is only applied to source code (file) names matching this value. The name may optionally
     * contain a path. If a path is specified, then the source code path must match it. If no path is
     * specified, then only the source code (file) name is compared (i.e., its path is ignored).
     * The value may optionally be a comma-separated list of names, in which case one of the names must match.
     * The name(s) may optionally include wildcard characters ('*' or '?').
     */
    String applyToFileNames

    /**
     * This rule is NOT applied to source code (file) names matching this value. The name may optionally
     * contain a path. If a path is specified, then the source code path must match it. If no path is
     * specified, then only the source code (file) name is compared (i.e., its path is ignored).
     * The value may optionally be a comma-separated list of names, in which case any one of the names can match.
     * The name(s) may optionally include wildcard characters ('*' or '?').
     */
    String doNotApplyToFileNames

    /**
     * If not null, this is used as the message for all violations of this rule, overriding any
     * message generated by the concrete rule subclass. Defaults to null. Note that setting this
     * to an empty string "hides" the message, if any, generated by the actual rule.
     */
    String violationMessage

    /**
     * If not null, this is used as the description text for this rule, overriding any
     * description text found in the i18n resource bundles. Defaults to null.
     */
    String description

    /**
     * @return the unique name for this rule
     */
    abstract String getName()

    /**
     * Set the unique name for this rule
     * @param name - the name for this rule; this should be unique 
     */
    abstract void setName(String name)

    /**
     * @return the priority of this rule, between 1 (highest priority) and 3 (lowest priority), inclusive.
     */
    abstract int getPriority()

    /**
     * Set the priority for this rule
     * @param priority - the priority of this rule, between 1 (highest priority) and 3 (lowest priority), inclusive.
     */
    abstract void setPriority(int priority)

    /**
     * Apply this rule to the specified source and return a list of violations (or an empty List)
     * @param source - the source to apply this rule to
     * @param violations - the List of violations to which new violations from this rule are to be added
     */
    abstract void applyTo(SourceCode sourceCode, List violations)

    /**
     * Apply this rule to the specified source and return a list of violations (or an empty List).
     * This implementation delegates to the abstract applyCode(SourceCode,List), provided by
     * concrete subclasses. This template method simplifies subclass implementations and also
     * enables common handling of enablement logic.
     * @param source - the source to apply this rule to
     * @return the List of violations; may be empty
     */
    List applyTo(SourceCode sourceCode) {
        try {
            validate()
            def violations = []
            if (shouldApplyThisRuleTo(sourceCode)) {
                applyTo(sourceCode, violations)
            }
            overrideViolationMessageIfNecessary(violations)
            return violations
        }
        catch(Throwable t) {
            LOG.error("Error from [${getClass().name}] processing source file [$sourceCode.path]", t)
            throw t
        }
    }

    /**
     * Allows rules to check whether preconditions are satisfied and short-circuit execution
     * (i.e., do nothing) if those preconditions are not satisfied. Return true by default.
     * This method is provided as a placeholder so subclasses can optionally override. 
     * @return true if all preconditions for this rule are satisfied
     */
    boolean isReady() {
        true
    }

    /**
     * Allows rules to perform validation. Do nothing by default.
     * This method is provided as a placeholder so subclasses can optionally override.
     * Subclasses will typically use <code>assert</code> calls to verify required preconditions.
     */
    @SuppressWarnings('EmptyMethodInAbstractClass')
    void validate() {
    }

    String toString() {
        "${getClassNameNoPackage()}[name=${getName()}, priority=${getPriority()}]"
    }

    /**
     * Create and return a new Violation for this rule and the specified values
     * @param lineNumber - the line number for the violation; may be null
     * @param sourceLine - the source line for the violation; may be null
     * @param message - the message for the violation; may be null
     * @return a new Violation object
     */
    protected Violation createViolation(Integer lineNumber, String sourceLine=null, String message=null) {
        new Violation(rule:this, sourceLine:sourceLine, lineNumber:lineNumber, message:message)
    }

    /**
     * Create and return a new Violation for this rule and the specified import
     * @param sourceCode - the SourceCode
     * @param importNode - the ImportNode for the import triggering the violation
     * @return a new Violation object
     */
    protected Violation createViolationForImport(SourceCode sourceCode, ImportNode importNode, String message = null) {
        def importInfo = sourceLineAndNumberForImport(sourceCode, importNode)
        new Violation(rule:this, sourceLine:importInfo.sourceLine, lineNumber:importInfo.lineNumber, message: message)
    }

    /**
     * Create and return a new Violation for this rule and the specified import className and alias
     * @param sourceCode - the SourceCode
     * @param className - the class name (as specified within the import statemenet)
     * @param alias - the alias for the import statemenet
     * @return a new Violation object
     */
    protected Violation createViolationForImport(SourceCode sourceCode, String className, String alias) {
        def importInfo = sourceLineAndNumberForImport(sourceCode, className, alias)
        new Violation(rule:this, sourceLine:importInfo.sourceLine, lineNumber:importInfo.lineNumber)
    }

    /**
     * Create a new Violation for the AST node.
     * @param sourceCode - the SourceCode
     * @param node - the Groovy AST Node
     * @param message - the message for the violation; defaults to null
     */
    protected Violation createViolation(SourceCode sourceCode, ASTNode node, message=null) {
        def sourceLine = sourceCode.line(node.lineNumber-1)
        createViolation(node.lineNumber, sourceLine, message)
    }

    protected List getImportsSortedByLineNumber(sourceCode) {
        sourceCode.ast.imports.sort { importNode ->
            def importInfo = sourceLineAndNumberForImport(sourceCode, importNode)
            importInfo.lineNumber
        }
    }

    /**
     * Return the source line and line number for the specified import class name and alias
     * @param sourceCode - the SourceCode being processed
     * @param importNode - the ImportNode representing the import
     * @return an object that has 'sourceLine' and 'lineNumber' fields
     */
    protected sourceLineAndNumberForImport(SourceCode sourceCode, String className, String alias) {
        // NOTE: This won't properly handle the case of multiple imports for same class if not all are aliased
        def index = sourceCode.lines.findIndexOf { line ->
            line.contains('import') &&
                line.contains(className) &&
                line.contains(alias)
        }
        def lineNumber = index == -1 ? null : index + 1
        def sourceLine = lineNumber == null ? "import $className as $alias".toString() : sourceCode.lines[lineNumber-1].trim()
        [sourceLine:sourceLine, lineNumber:lineNumber]
    }

    /**
     * Return the source line and line number for the specified import class name and alias
     * @param sourceCode - the SourceCode being processed
     * @param importNode - the ImportNode representing the import
     * @return an object that has 'sourceLine' and 'lineNumber' fields
     */
    protected sourceLineAndNumberForStarImport(SourceCode sourceCode, ImportNode importNode) {
        if (!importNode.isStar()) {
            return [sourceLine:-1, lineNumber:-1]
        }
        // NOTE: This won't properly handle the case of multiple imports for same class if not all are aliased
        def index = sourceCode.lines.findIndexOf { line ->
            line.contains('import') &&
                line.contains(importNode.packageName + '*')
        }
        def lineNumber = index == -1 ? null : index + 1
        def sourceLine = lineNumber == null ? "import ${importNode.packageName}*".toString() : sourceCode.lines[lineNumber-1].trim()
        [sourceLine:sourceLine, lineNumber:lineNumber]
    }

    /**
     * Return the source line and line number for the specified import
     * @param sourceCode - the SourceCode being processed
     * @param importNode - the ImportNode representing the import
     * @return an object that has 'sourceLine' and 'lineNumber' fields
     */
    protected sourceLineAndNumberForImport(SourceCode sourceCode, ImportNode importNode) {
        if (importNode.isStar()) {
            sourceLineAndNumberForStarImport(sourceCode, importNode)
        } else {
            sourceLineAndNumberForImport(sourceCode, importNode.className, importNode.alias)
        }
    }

    /**
     * Return the package name for the specified import statement or else an empty String
     * @param importNode - the ImportNode for the import
     * @return the name package being imported (i.e., the import minus the class name/spec)
     *      or an empty String if the import contains no package component
     */
    protected String packageNameForImport(ImportNode importNode) {
        def importClassName = importNode.className
        def index = importClassName.lastIndexOf('.')
        (index == -1) ? '' : importClassName.substring(0, index) 
    }

    private boolean shouldApplyThisRuleTo(SourceCode sourceCode) {
        // TODO Consider caching SourceCodeCriteria instance 
        enabled && isReady() &&
            new SourceCodeCriteria(
                applyToFilesMatching:getProperty('applyToFilesMatching'),
                doNotApplyToFilesMatching:getProperty('doNotApplyToFilesMatching'),
                applyToFileNames:getProperty('applyToFileNames'),
                doNotApplyToFileNames:getProperty('doNotApplyToFileNames')).matches(sourceCode)
    }

    private String getClassNameNoPackage() {
        def className = getClass().name
        def indexOfLastPeriod = className.lastIndexOf('.')
        (indexOfLastPeriod == -1) ? className : className.substring(indexOfLastPeriod+1)
    }

    /**
     * If the violationMessage property of this rule has been set, then use it to set the
     * message within each violation, overriding the original message(s), if any.
     */
    private void overrideViolationMessageIfNecessary(List violations) {
        if (violationMessage != null) {
            violations.each {violation -> violation.message = violationMessage }
        }
    }

}