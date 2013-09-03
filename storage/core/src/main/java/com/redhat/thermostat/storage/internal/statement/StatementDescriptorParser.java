/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.storage.internal.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.SortDirection;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryComparisonOperator;
import com.redhat.thermostat.storage.query.BinaryLogicalOperator;

/**
 * A parser for the string representation of {@link StatementDescriptor}s.
 * Tokens have to be separated by whitespace.
 * 
 * This parser implements the following simple grammar for statement descriptors.
 * It supports the following statement types:
 * <ul>
 * <li>QUERY (read)</li>
 * <li>QUERY-COUNT (read)</li>
 * <li>ADD (write)</li>
 * <li>UPDATE (write)</li>
 * <li>REPLACE (write)</li>
 * <li>REMOVE (write)</li>
 * </ul>
 * 
 * <p><strong>Grammar:</strong></p>
 * <pre>
 * statementDesc := statementType category setList suffix
 * statementType := 'QUERY' | 'QUERY-COUNT' |
 *                  'ADD' | 'REPLACE' | 'UPDATE' |
 *                  'REMOVE'
 * category      := string
 * setList       := 'SET' setValues | \empty
 * setValues     := valuePair valueList
 * valuePair     := term '=' term
 * valueList     := ',' setValues | \empty
 * suffix        := 'WHERE' where |
 *                  'SORT' sortCond |
 *                  'LIMIT' term | \empty
 * where         := whereExp sort limit
 * whereExp      := andCond orCond
 * orCond        := 'OR' whereExp | \empty
 * sort          := 'SORT' sortCond | \empty
 * sortCond      := sortPair sortList
 * sortPair      := term sortModifier
 * sortModifier  := 'ASC' | 'DSC'
 * sortList      := ',' sortCond | \empty
 * limit         := 'LIMIT' term | \empty
 * andCond       := condition andBody
 * andBody       := 'AND' whereExp | \empty
 * condition     := 'NOT' condition | compExp
 * compExp       := term compExpRHS
 * term          := freeParam | literal
 * freeParam     := '?s' | '?i' | '?l' | '?s[' | '?b'
 * literal       := sQuote string sQuote | int | long | boolean
 * sQuote        := \'
 * boolean       := &lt;true&gt; | &lt;false&gt;
 * int           := &lt;literal-int&gt;
 * long          := &lt;literal-long&gt;longPostFix
 * longPostFix   := 'l' | 'L'
 * string        := &lt;literal-string-value&gt;
 * compExpRHS    := '!=' term | '=' term | '&lt;=' term | '&gt;=' term | 
 *                  '&lt;' term | '&gt;' term
 * </pre>
 *
 * This implements the following logic precedence rules (in this order of
 * precedence):
 * 
 * <ol>
 *   <li>NOT</li>
 *   <li>AND</li>
 *   <li>OR</li>
 * </ol>
 * 
 * NOTE: Comparison expressions have equal precedence.
 */
class StatementDescriptorParser<T extends Pojo> {

    private static final String TOKEN_DELIMS = " \t\r\n\f";
    private static final short IDX_QUERY = 0;
    private static final short IDX_QUERY_COUNT = 1;
    private static final short IDX_ADD = 2;
    private static final short IDX_REPLACE = 3;
    private static final short IDX_UPDATE = 4;
    private static final short IDX_REMOVE = 5;
    private static final String[] KNOWN_STATEMENT_TYPES = new String[] {
        "QUERY", "QUERY-COUNT", "ADD", "REPLACE", "UPDATE", "REMOVE"
    };
    private static final String SORTLIST_SEP = ",";
    private static final String SETLIST_SEP = SORTLIST_SEP;
    private static final String KEYWORD_SET = "SET";
    private static final String KEYWORD_WHERE = "WHERE";
    private static final String KEYWORD_SORT = "SORT";
    private static final String KEYWORD_LIMIT = "LIMIT";
    private static final String KEYWORD_ASC = "ASC";
    private static final String KEYWORD_DSC = "DSC";
    private static final char PARAM_PLACEHOLDER = '?';
    
    private final String[] tokens;
    private final StatementDescriptor<T> desc;
    private final BackingStorage storage;
    private int currTokenIndex;
    private int placeHolderCount;
    // the parsed statement
    private ParsedStatementImpl<T> parsedStatement;
    private SuffixExpression tree;
    private SetList setList;
    
    StatementDescriptorParser(BackingStorage storage, StatementDescriptor<T> desc) {
        this.tokens = getTokens(desc.getDescriptor());
        this.currTokenIndex = 0;
        this.placeHolderCount = 0;
        this.desc = desc;
        this.storage = storage;
    }
    
    private String[] getTokens(String str) {
        StringTokenizer tokenizer = new StringTokenizer(str, TOKEN_DELIMS);
        List<String> toks = new ArrayList<>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            toks.add(tokenizer.nextToken());
        }
        return toks.toArray(new String[0]);
    }

    public ParsedStatement<T> parse() throws DescriptorParsingException {
        matchStatementType();
        matchCategory();
        // matched so far, create the raw statement
        createStatement();
        this.setList = new SetList();
        matchSetList(setList);
        this.tree = new SuffixExpression();
        matchSuffix();
        if (currTokenIndex != tokens.length) {
            throw new DescriptorParsingException("Incomplete parse");
        }
        parsedStatement.setNumFreeParams(placeHolderCount);
        parsedStatement.setSetList(setList);
        parsedStatement.setSuffixExpression(tree);
        doSemanticAnalysis();
        return parsedStatement;
    }

    private void doSemanticAnalysis() throws DescriptorParsingException {
        // TODO:
        // - Check that ADD/REPLACE specifies all keys judging by the Pojo
        //   model class. Not sure if good idea though, as this would likely
        //   introduce dep on beanutils.
        Statement<T> stmt = parsedStatement.getRawStatement();
        if (stmt == null) {
            // should never be null
            throw new NullPointerException();
        }
        if (stmt instanceof Add && tree.getWhereExpn() != null) {
            String msg = "WHERE clause not allowed for ADD";
            throw new DescriptorParsingException(msg);
        }
        if (stmt instanceof Replace && tree.getWhereExpn() == null) {
            String msg = "WHERE clause required for REPLACE";
            throw new DescriptorParsingException(msg);
        }
        if (stmt instanceof Update) {
            if (tree.getWhereExpn() == null) {
                // WHERE required for UPDATE
                String msg = "WHERE clause required for UPDATE";
                throw new DescriptorParsingException(msg);
            }
            if (setList.getValues().size() == 0) {
                // SET required for UPDATE
                String msg = "SET list required for UPDATE";
                throw new DescriptorParsingException(msg);
            }
        }
        if (stmt instanceof Remove && setList.getValues().size() > 0) {
            String msg = "SET not allowed for REMOVE";
            throw new DescriptorParsingException(msg);
        }
        if (stmt instanceof Query) {
            if (setList.getValues().size() > 0) {
                // Must not have SET for QUERYs
                String msg = "SET not allowed for QUERY/QUERY-COUNT";
                throw new DescriptorParsingException(msg);
            }
        } else {
            // only queries can have sort/limit expressions
            if (this.tree.getLimitExpn() != null || this.tree.getSortExpn() != null) {
                String msg = "LIMIT/SORT only allowed for QUERY/QUERY-COUNT";
                throw new DescriptorParsingException(msg);
            }
        }
    }

    /*
     * Match set list for DML statements.
     */
    private void matchSetList(final SetList setList) throws DescriptorParsingException {
        if (tokens.length == currTokenIndex) {
            // no set list
            return;
        }
        if (tokens[currTokenIndex].equals(KEYWORD_SET)) {
            currTokenIndex++; // SET
            matchSetValues(setList);
        }
        // empty, proceed with suffix
    }

    /*
     * Match list of values in a SET expression 
     */
    private void matchSetValues(SetList setList) throws DescriptorParsingException {
        matchValuePair(setList);
        matchValueList(setList);
    }

    /*
     * Match more value pairs in a SET list
     */
    private void matchValueList(SetList setList) throws DescriptorParsingException {
        if (currTokenIndex == tokens.length) {
            // empty
            return;
        }
        if (tokens[currTokenIndex].equals(SETLIST_SEP)) {
            currTokenIndex++; // ,
            matchSetValues(setList);
        }
    }

    /*
     * Match one pair of values in a 
     */
    private void matchValuePair(SetList setList) throws DescriptorParsingException {
        SetListValue value = new SetListValue();
        TerminalNode lval = new TerminalNode(null);
        matchTerm(lval, true);
        value.setKey(lval);
        if (tokens[currTokenIndex].equals("=")) {
            currTokenIndex++; // =
        } else {
            String msg = "Expected '=' after SET value LHS. Token was ->" + tokens[currTokenIndex] + "<-";
            throw new DescriptorParsingException(msg);
        }
        TerminalNode rval = new TerminalNode(null);
        matchTerm(rval, false);
        value.setValue(rval);
        setList.addValue(value);
    }

    /*
     * Match optional suffixes. 
     */
    private void matchSuffix() throws DescriptorParsingException {
        if (tokens.length == currTokenIndex) {
            // no suffix
            return;
        }
        if (tokens[currTokenIndex].equals(KEYWORD_WHERE)) {
            currTokenIndex++;
            WhereExpression expn = new WhereExpression();
            tree.setWhereExpn(expn);
            matchWhereExp(expn.getRoot());
            matchSort(tree);
            matchLimit(tree);
        } else if (tokens[currTokenIndex].equals(KEYWORD_SORT)) {
            // SORT token eaten up by matchSort()
            matchSort(tree);
            matchLimit(tree);
        } else if (tokens[currTokenIndex].equals(KEYWORD_LIMIT)) {
            // LIMIT token eaten up by matchLimit()
            matchLimit(tree);
        } else {
            throw new DescriptorParsingException("Unexpected token: '"
                    + tokens[currTokenIndex] + "'. Expected one of "
                    + KEYWORD_WHERE + ", " + KEYWORD_SORT + ", " + KEYWORD_LIMIT);
        }
    }

    private void matchLimit(SuffixExpression tree) throws DescriptorParsingException {
        if (currTokenIndex == tokens.length) {
            // empty
            return;
        } else if (currTokenIndex < tokens.length) {
            if (tokens[currTokenIndex].equals(KEYWORD_LIMIT)) {
                LimitExpression node = new LimitExpression();
                tree.setLimitExpn(node);
                currTokenIndex++;
                matchTerm(node);
            }
        } else {
            throw new DescriptorParsingException("Illegal statement descriptor: Reason LIMIT");
        }
    }

    private void matchSort(SuffixExpression tree) throws DescriptorParsingException {
        if (currTokenIndex < tokens.length
                && tokens[currTokenIndex].equals(KEYWORD_SORT)) {
            SortExpression sortExpn = new SortExpression();
            tree.setSortExpn(sortExpn);
            currTokenIndex++;
            matchSortList(sortExpn);
        } 
        if (currTokenIndex > tokens.length) {
            throw new DescriptorParsingException("Illegal statement descriptor.");
        }
        // empty
    }

    private void matchSortList(SortExpression sortExpn) throws DescriptorParsingException {
        matchSortPair(sortExpn);
        matchSortListPreamble(sortExpn);
    }

    private void matchSortListPreamble(SortExpression sortExpn) throws DescriptorParsingException {
        if (currTokenIndex < tokens.length && tokens[currTokenIndex].equals(SORTLIST_SEP)) {
            currTokenIndex++; // ',' token
            matchSortList(sortExpn);
        }
    }

    private void matchSortPair(SortExpression expn) throws DescriptorParsingException {
        SortMember member = new SortMember();
        matchTerm(member);
        matchSortModifier(member);
        // Add the member node to the list of the sort node
        expn.addMember(member);
    }

    private void matchSortModifier(SortMember member) throws DescriptorParsingException {
        String msg = "Illegal statement decriptor: Reason SORT. Expected ASC or DSC";
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException(msg);
        }
        if (tokens[currTokenIndex].equals(KEYWORD_ASC)) {
            member.setDirection(SortDirection.ASCENDING);
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KEYWORD_DSC)) {
            member.setDirection(SortDirection.DESCENDING);
            currTokenIndex++;
        } else {
            throw new DescriptorParsingException(msg);
        }
    }

    private void matchWhereExp(Node node) throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException("Illegal where clause");
        }
        assert(node != null);
        matchAndCondition(node);
        matchOrCondition(node);
    }

    private void matchAndCondition(Node currNode) throws DescriptorParsingException {
        matchCondition(currNode);
        matchAndExpression(currNode);
    }

    private void matchCondition(Node currNode) throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException("Illegal statement descriptor: Reason sort clause");
        }
        if (tokens[currTokenIndex].equals(Operator.NOT.getName())) {
            NotBooleanExpressionNode notNode = new NotBooleanExpressionNode(currNode);
            if (currNode instanceof BinaryExpressionNode) {
                BinaryExpressionNode currNodeExpr = (BinaryExpressionNode)currNode;
                Node available = currNodeExpr.getLeftChild();
                if (available != null) {
                    currNodeExpr.setRightChild(notNode);
                } else {
                    currNodeExpr.setLeftChild(notNode);
                }
            } else {
                assert(currNode instanceof NotBooleanExpressionNode || currNode instanceof Node);
                currNode.setValue(notNode);
            }
            currTokenIndex++; // NOT keyword
            
            matchCondition(notNode);
        } else {
            matchComparisionExpression(currNode);
        }
    }

    private void matchComparisionExpression(Node currNode) throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException("Illegal statement descriptor: Comparison expression");
        }
        BinaryExpressionNode expr = new BinaryExpressionNode(currNode);
        TerminalNode left = new TerminalNode(expr);
        TerminalNode right = new TerminalNode(expr);
        expr.setLeftChild(left);
        expr.setRightChild(right);
        
        if (currNode instanceof BinaryExpressionNode) {
            BinaryExpressionNode currNodeExpr = (BinaryExpressionNode)currNode;
            Node available = currNodeExpr.getLeftChild();
            if (available == null) {
                currNodeExpr.setLeftChild(expr);
            } else {
                assert(currNodeExpr.getRightChild() == null);
                currNodeExpr.setRightChild(expr);
            }
        } else {
            assert(currNode instanceof NotBooleanExpressionNode || currNode instanceof Node);
            currNode.setValue(expr);
        }
        
        matchTerm(left, true);
        matchComparisonRHS(expr);
    }

    private void matchComparisonRHS(BinaryExpressionNode currNode) throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            // boolean literals are not allowed
            throw new DescriptorParsingException("Illegal statement descriptor: Boolean literals are not allowed!");
        }
        if (tokens[currTokenIndex].equals(Operator.EQUALS.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.EQUALS);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else if (tokens[currTokenIndex].equals(Operator.LESS_THAN_OR_EQUAL_TO.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.LESS_THAN_OR_EQUAL_TO);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else if (tokens[currTokenIndex].equals(Operator.GREATER_THAN_OR_EQUAL_TO.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.GREATER_THAN_OR_EQUAL_TO);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else if (tokens[currTokenIndex].equals(Operator.GREATER_THAN.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.GREATER_THAN);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else if (tokens[currTokenIndex].equals(Operator.LESS_THAN.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.LESS_THAN);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else if (tokens[currTokenIndex].equals(Operator.NOT_EQUAL_TO.getName())) {
            currTokenIndex++;
            currNode.setOperator(BinaryComparisonOperator.NOT_EQUAL_TO);
            matchTerm((TerminalNode)currNode.getRightChild(), false);
        } else {
            throw new DescriptorParsingException("Illegal statement descriptor: Reason comparison expression!");
        }
    }
    
    private void matchTerm(SortMember member) throws DescriptorParsingException {
        String term = getTerm();
        if (term.charAt(0) == PARAM_PLACEHOLDER) {
            assert(placeHolderCount > 0);
            if (term.charAt(1) != 's') {
                String msg = "Sort parameters only accept string types. Placeholder was: " + term;
                throw new DescriptorParsingException(msg);
            }
            UnfinishedSortKey unfinishedKey = new UnfinishedSortKey();
            unfinishedKey.setParameterIndex(placeHolderCount - 1);
            member.setSortKey(unfinishedKey);
            return;
        }
        String stringTerm = getStringTerm(term);
        member.setSortKey(stringTerm);
    }
    
    private void matchTerm(LimitExpression expn) throws DescriptorParsingException {
        String term = getTerm();
        if (term.charAt(0) == PARAM_PLACEHOLDER) {
            assert(placeHolderCount > 0);
            if (term.charAt(1) != 'i') {
                String msg = "Limit parameters only accept integer types. Placeholder was: " + term;
                throw new DescriptorParsingException(msg);
            }
            UnfinishedLimitValue limitValue = new UnfinishedLimitValue();
            limitValue.setParameterIndex(placeHolderCount - 1);
            expn.setValue(limitValue);
            return;
        }
        int limitVal;
        try {
            limitVal = Integer.parseInt(term);
        } catch (NumberFormatException e) {
            throw new DescriptorParsingException("Invalid limit expression. '" + term + "' not an integer");
        }
        expn.setValue(limitVal);
    }

    private void matchTerm(TerminalNode node, boolean isLHS) throws DescriptorParsingException {
        String term = getTerm();
        if (term.charAt(0) == PARAM_PLACEHOLDER) {
            assert(placeHolderCount > 0);
            UnfinishedValueNode patchNode = new UnfinishedValueNode();
            patchNode.setParameterIndex(placeHolderCount - 1);
            patchNode.setLHS(isLHS);
            // figure out the expected type
            Class<?> expectedType = getType(term.substring(1));
            if (expectedType == null) {
                throw new DescriptorParsingException("Unknown type of free parameter: '" + term + "'");
            }
            patchNode.setType(expectedType);
            node.setValue(patchNode);
            return;
        }
        // regular terminal. i.e. literal value
        if (isLHS) {
            // FIXME: In thermostat LHS of comparisons must be Key objects. I'm
            // not sure if this restriction is very meaningful in a prepared
            // statement context as the purpose of this was to ensure "type"
            // compatibility between Key <=> value comparisons.
            String stringTerm = getStringTerm(term);
            Key<?> key = new Key<>(stringTerm);
            node.setValue(key);
        } else {
            Object typedValue = getTypedValue(term);
            node.setValue(typedValue);
        }
    }
    
    private Object getTypedValue(String term) throws DescriptorParsingException {
        try {
            String stringTerm = getStringTerm(term);
            return stringTerm;
        } catch (DescriptorParsingException e) {
            // Must be integer (long/int) or boolean. First check for boolean,
            // then for long and regular ints.
            if (term.equals(Boolean.toString(false)) || term.equals(Boolean.toString(true))) {
                boolean boolVal = Boolean.parseBoolean(term);
                return boolVal;
            }
            // Next, parse long or int.
            try {
                int lastCharInTokenIndex = term.length() - 1;
                // preceding l/L indicate long integer types.
                if (term.charAt(lastCharInTokenIndex) == 'L' || term.charAt(lastCharInTokenIndex) == 'l') {
                    long longVal = Long.parseLong(term.substring(0, lastCharInTokenIndex));
                    return longVal;
                }
                // must be integer or some invalid type
                int intVal = Integer.parseInt(term);
                return intVal;
            } catch (NumberFormatException nfe) {
                String msg = "Illegal terminal type. Token was ->" + term + "<-";
                throw new DescriptorParsingException(msg);
            }
        }
    }

    private String getStringTerm(String term) throws DescriptorParsingException {
        String errorMsg = 
                "Expected string value. Got term ->"
                        + term
                        + "<-"
                        + " Was the string properly quoted? Example: 'string'.";
        if (term.charAt(0) != '\'' || term.charAt(term.length() - 1) != '\'') {
            throw new DescriptorParsingException(errorMsg);
        }
        return term.substring(1, term.length() - 1);
    }

    private Class<?> getType(String term) {
        if (term.equals("")) {
            // illegal type
            return null;
        }
        assert(term.equals("i") || term.equals("l") || term.equals("s") || term.equals("s[") || term.equals("b"));
        char switchChar = term.charAt(0);
        Class<?> type = null;
        switch (switchChar) {
        case 'i': {
            type = Integer.class;
            break;
        }
        case 'l': {
            type = Long.class;
            break;
        }
        case 's': {
            if (term.length() == 1) {
                type = String.class;
            } else if (term.length() == 2 && term.charAt(1) == '[') {
                type = String[].class;
            }
            break;
        }
        case 'b': {
            type = Boolean.class;
            break;
        }
        default:
            assert (type == null);
            break;
        }
        return type;
    }

    private String getTerm() throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException("Invalid where clause. Reason: term expected but not given!");
        }
        if (tokens[currTokenIndex].charAt(0) == PARAM_PLACEHOLDER) {
                placeHolderCount++;
        }
        String term = tokens[currTokenIndex];
        currTokenIndex++;
        return term;
    }

    private void matchAndExpression(Node currNode) throws DescriptorParsingException {
        if (currTokenIndex < tokens.length &&
                tokens[currTokenIndex].equals(Operator.AND.getName())) {
            currTokenIndex++; // AND keyword
            
            Node parent = currNode;
            if (currNode instanceof BinaryExpressionNode ||
                    currNode instanceof NotBooleanExpressionNode) {
                parent = currNode.getParent();
                assert(parent != null);
            }
            BinaryExpressionNode and = new BinaryExpressionNode(parent);
            and.setOperator(BinaryLogicalOperator.AND);
            if (currNode instanceof BinaryExpressionNode ||
                    currNode instanceof NotBooleanExpressionNode) {
                currNode.setParent(and);
                and.setLeftChild(currNode);
                parent.setValue(and);
            } else {
                // Root node case
                and.setLeftChild((Node)parent.getValue());
                parent.setValue(and);
            }
            // Note the current AND expression node at this point of parsing
            // must be at the root of the entire expression.
            assert(and.getParent().getParent() == null);
            
            matchWhereExp(and);
            
        }
        // empty
    }

    private void matchOrCondition(Node currNode) throws DescriptorParsingException {
        if (currTokenIndex < tokens.length &&
                tokens[currTokenIndex].equals(Operator.OR.getName())) {
            currTokenIndex++; // OR keyword
            
            Node parent = currNode;
            if (currNode instanceof BinaryExpressionNode ||
                    currNode instanceof NotBooleanExpressionNode) {
                parent = currNode.getParent();
                assert(parent != null);
            }
            BinaryExpressionNode or = new BinaryExpressionNode(parent);
            or.setOperator(BinaryLogicalOperator.OR);
            if (currNode instanceof BinaryExpressionNode ||
                    currNode instanceof NotBooleanExpressionNode) {
                currNode.setParent(or);
                or.setLeftChild(currNode);
                parent.setValue(or);
            } else {
                // Root node case
                or.setLeftChild((Node)parent.getValue());
                parent.setValue(or);
            }
            // Note the current OR expression node at this point of parsing
            // must be at the root of the entire expression.
            assert(or.getParent().getParent() == null);
            
            matchWhereExp(or);
        }
        // empty
    }

    private void createStatement() {
        // matchStatementType and matchCategory advanced currTokenIndex,
        // lets use idx of 0 here.
        final String statementType = tokens[0];
        Class<T> dataClass = desc.getCategory().getDataClass();
        if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_QUERY])) {
            // regular query case
            Query<T> query = storage.createQuery(desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(query, dataClass);
        } else if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_QUERY_COUNT])) {
            // create aggregate count query
            Query<T> query = storage.createAggregateQuery(AggregateFunction.COUNT, desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(query, dataClass);
        } else if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_ADD])) {
            // create add
            Add<T> add = storage.createAdd(desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(add, dataClass);
        } else if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_REPLACE])) {
            // create replace
            Replace<T> replace = storage.createReplace(desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(replace, dataClass);
        } else if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_UPDATE])) {
            // create replace
            Update<T> update = storage.createUpdate(desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(update, dataClass);
        } else if (statementType.equals(KNOWN_STATEMENT_TYPES[IDX_REMOVE])) {
            // create remove
            Remove<T> remove = storage.createRemove(desc.getCategory());
            this.parsedStatement = new ParsedStatementImpl<>(remove, dataClass);
        } else {
            throw new IllegalStateException("Don't know how to create statement type '" + statementType + "'");
        }
    }

    private void matchCategory() throws DescriptorParsingException {
        if (currTokenIndex >= tokens.length) {
            throw new DescriptorParsingException("Missing category name in descriptor: '" + desc.getDescriptor() + "'");
        }
        Category<?> category = desc.getCategory();
        if (!tokens[currTokenIndex].equals(category.getName())) {
            throw new DescriptorParsingException(
                    "Category mismatch in descriptor. Category from descriptor string: '"
                            + tokens[currTokenIndex]
                            + "'. Category name from category: '"
                            + category.getName() + "'.");
        }
        currTokenIndex++;
    }

    private void matchStatementType() throws DescriptorParsingException {
        if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_QUERY])) {
            // QUERY
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_QUERY_COUNT])) {
            // QUERY-COUNT
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_ADD])) {
            // ADD
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_REPLACE])) {
            // REPLACE
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_UPDATE])) {
            // UPDATE
            currTokenIndex++;
        } else if (tokens[currTokenIndex].equals(KNOWN_STATEMENT_TYPES[IDX_REMOVE])) {
            // REMOVE
            currTokenIndex++;
        } else {
            throw new DescriptorParsingException("Unknown statement type: '" + tokens[currTokenIndex] + "'");
        }
    }
}
