package com.intellij.tapestry.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import static com.intellij.tapestry.psi.TelTokenTypes.*;
import static com.intellij.tapestry.psi.TelCompositeElementType.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexey Chmutov
 *         Date: Jun 22, 2009
 *         Time: 9:53:11 PM
 */
public class TelParser implements PsiParser {
  @NotNull
  public ASTNode parse(IElementType root, PsiBuilder builder) {
    //builder.setDebugMode(true);
    final ASTNode contextNode = builder.getUserData(TAP5_CONTEXT_NODE_KEY);
    final PsiBuilder.Marker rootMarker = builder.mark();
    final boolean elUnderFile = contextNode != null && contextNode.getElementType() == TEL_FILE;
    final PsiBuilder.Marker markerUnderFile = elUnderFile ? builder.mark() : null;
    while (!builder.eof()) {
      parseExpression(builder);
    }
    if (markerUnderFile != null) {
      markerUnderFile.done(root);
      rootMarker.done(TEL_FILE);
    }
    else {
      rootMarker.done(root);
    }
    return builder.getTreeBuilt();
  }

  public static void parseExpression(PsiBuilder builder) {
    if (consumeToken(builder, TAP5_EL_START)) {
      PsiBuilder.Marker referenceExpression = consumeIdentifierAndMark(builder);
      if (referenceExpression != null) {
        if(consumeOptionalToken(builder, TAP5_EL_COLON)) {
          try {
            parseExpressionInner(builder);
          }
          finally {
            referenceExpression.done(EXPLICIT_BINDING);
          }
        } else {
          parsePropertyChainTrailer(builder, referenceExpression);
        }
      }
      else {
        parseExpressionInner(builder);
      }
      consumeToken(builder, TAP5_EL_END);
    }
    else {
      builder.advanceLexer();
    }
  }

  private static boolean parseExpressionInner(PsiBuilder builder) {
    PsiBuilder.Marker mark = builder.mark();
    TelCompositeElementType res = null;
    try {
      if (consumeOptionalToken(builder, TAP5_EL_LEFT_BRACKET)) {
        res = LIST_EXPRESSION;
        parseExpressionList(builder);
        consumeToken(builder, TAP5_EL_RIGHT_BRACKET);
        return true;
      }
      if (consumeOptionalToken(builder, TAP5_EL_EXCLAMATION)) {
        res = NOT_OP_EXPRESSION;
        parseExpressionInner(builder);
        return true;
      }
      res = parseConstantExpr(builder);
      boolean propertyChainFound = false;
      if (res == null) {
        propertyChainFound = parsePropertyChainExpression(builder);
      }
      if ((propertyChainFound || res == INTEGER_LITERAL) && builder.getTokenType() == TAP5_EL_RANGE) {
        if (res != null) {
          mark.done(res);
          mark = mark.precede();
        }
        consumeToken(builder, TAP5_EL_RANGE);
        res = RANGE_EXPRESSION;
        if (!parseIntegerLiteral(builder) && !parsePropertyChainExpression(builder)) {
          builder.error("property chain or integer literal expected");
        }
      }
      return propertyChainFound || res != null;
    }
    finally {
      if (res != null) {
        mark.done(res);
      }
      else {
        mark.drop();
      }
    }
  }

  private static boolean parseIntegerLiteral(PsiBuilder builder) {
    PsiBuilder.Marker mark = builder.mark();
    final boolean result = consumeOptionalToken(builder, TAP5_EL_INTEGER);
    if (result) {
      mark.done(INTEGER_LITERAL);
    }
    else {
      mark.drop();
    }
    return result;
  }

  private static boolean consumeOptionalToken(PsiBuilder builder, TelTokenType tokenType) {
    if (tokenType != builder.getTokenType()) return false;
    builder.advanceLexer();
    return true;
  }

  private static boolean consumeToken(PsiBuilder builder, TelTokenType tokenType) {
    if (tokenType != builder.getTokenType()) {
      String s = tokenType.toString();
      if (s.startsWith("TAP5_EL_")) s = s.substring("TAP5_EL_".length());
      builder.error(s + " expected");
      return false;
    }
    builder.advanceLexer();
    return true;
  }

  @Nullable
  private static TelCompositeElementType parseConstantExpr(PsiBuilder builder) {
    if (consumeOptionalToken(builder, TAP5_EL_BOOLEAN)) {
      return BOOLEAN_LITERAL;
    }
    if (consumeOptionalToken(builder, TAP5_EL_INTEGER)) {
      return INTEGER_LITERAL;
    }
    if (consumeOptionalToken(builder, TAP5_EL_DECIMAL)) {
      return DECIMAL_LITERAL;
    }
    if (consumeOptionalToken(builder, TAP5_EL_STRING)) {
      return STRING_LITERAL;
    }
    if (consumeOptionalToken(builder, TAP5_EL_NULL)) {
      return NULL_LITERAL;
    }
    return null;
  }

  private static boolean parsePropertyChainExpression(PsiBuilder builder) {
    PsiBuilder.Marker referenceExpression = consumeIdentifierAndMark(builder);
    if (referenceExpression == null) return false;
    parsePropertyChainTrailer(builder, referenceExpression);
    return true;
  }

  @Nullable
  private static PsiBuilder.Marker consumeIdentifierAndMark(PsiBuilder builder) {
    if (TAP5_EL_IDENTIFIER != builder.getTokenType()) return null;
    PsiBuilder.Marker mark = builder.mark();
    builder.advanceLexer();
    return mark;
  }

  private static void parsePropertyChainTrailer(PsiBuilder builder, PsiBuilder.Marker referenceExpression) {
    referenceExpression.done(REFERENCE_EXPRESSION);
    referenceExpression = referenceExpression.precede();
    while (consumeOptionalToken(builder, TAP5_EL_DOT) || consumeOptionalToken(builder, TAP5_EL_QUESTION_DOT)) {
      if (!consumeToken(builder, TAP5_EL_IDENTIFIER)) break;
      referenceExpression.done(REFERENCE_EXPRESSION);
      if (consumeOptionalToken(builder, TAP5_EL_LEFT_PARENTH)) {
        referenceExpression = referenceExpression.precede();
        parseExpressionList(builder);
        consumeToken(builder, TAP5_EL_RIGHT_PARENTH);
        referenceExpression.done(METHOD_CALL_EXPRESSION);
      }
      referenceExpression = referenceExpression.precede();
    }
    referenceExpression.drop();
  }

  private static void parseExpressionList(PsiBuilder builder) {
    if (!parseExpressionInner(builder)) return;
    while (consumeOptionalToken(builder, TAP5_EL_COMMA)) {
      if (!parseExpressionInner(builder)) {
        builder.error("expression expected");
      }
    }
  }
}
