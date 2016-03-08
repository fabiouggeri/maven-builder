/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Fabio
 */
public class CommandLineTokenizer {

   public List<CommandLineToken> parse(CharSequence str) throws CommandLineTokenizerException {
      final List<CommandLineToken> tokens = new ArrayList<CommandLineToken>();
      final StringBuilder text = new StringBuilder();
      int index = 0;
      int strLen = str.length();
      while (index < strLen) {
         final char c = charAt(str, index);
         switch(c) {
            case '#':
               addCurrentTextToken(text, tokens);
               index = consumeVariable(str, index, tokens);
               break;
            case '\\':
               if (charAt(str, index + 1) == '#') {
                  index += 2;
                  text.append('#');
                  break;
               }
            default:
               text.append(c);
               ++index;
               break;
         }
      }
      addCurrentTextToken(text, tokens);
      return tokens;
   }

   private void addCurrentTextToken(final StringBuilder text, final List<CommandLineToken> tokens) {
      if (text.length() > 0) {
         tokens.add(new TextToken(text));
         text.setLength(0);
      }
   }

   private int consumeVariable(CharSequence str, int index, List<CommandLineToken> tokens) throws CommandLineTokenizerException {
      final StringBuilder varName = new StringBuilder();
      final StringBuilder testValue = new StringBuilder();
      final StringBuilder initText = new StringBuilder();
      final StringBuilder elementSep = new StringBuilder();
      final StringBuilder keyValueSep = new StringBuilder();
      boolean propertyVariable = false;
      boolean appendPropertyVal = false;
      boolean testNotDefined = false;
      boolean testEqual = true;

      consume(str, index++, '#');
      index = ignoreSpaces(str, index);
      consume(str, index++, '{');
      index = ignoreSpaces(str, index);
      switch (charAt(str, index)) {
         case '?':
            propertyVariable = true;
            index = ignoreSpaces(str, ++index);
            break;
         case '&':
            appendPropertyVal = true;
            propertyVariable = true;
            index = ignoreSpaces(str, ++index);
            break;
         case '!':
            testNotDefined = true;
            propertyVariable = true;
            index = ignoreSpaces(str, ++index);
            break;
      }
      index = consumeVariableName(str, index, varName);
      index = ignoreSpaces(str, index);
      if (propertyVariable)  {
         if (charAt(str, index) == '=') {
            if (testNotDefined) {
               throw new CommandLineTokenizerException("Cannot test the define content with test definition. Unexpected character at " + index);
            }
            index = ignoreSpaces(str, ++index);
            if (charAt(str, index) != '\'') {
               throw new CommandLineTokenizerException("Expected character \"'\" not found at " + index);
            }
            index = consumeQuotedText(str, index, testValue);
            index = ignoreSpaces(str, index);
         } else if (charAt(str, index) == '!') {
            if (testNotDefined) {
               throw new CommandLineTokenizerException("Cannot test the define content with test definition. Unexpected character at " + index);
            }
            if (charAt(str, ++index) == '=') {
               testEqual = false;
               index = ignoreSpaces(str, ++index);
               if (charAt(str, index) != '\'') {
                  throw new CommandLineTokenizerException("Expected character \"'\" not found at " + index);
               }
               index = consumeQuotedText(str, index, testValue);
               index = ignoreSpaces(str, index);
            } else {
               throw new CommandLineTokenizerException("Expected character \"=\" not found at " + index);
            }
         }
      }
      if (charAt(str, index) == ',') {
         index = ignoreSpaces(str, ++index);
         index = consumeQuotedText(str, index, initText);
         index = ignoreSpaces(str, index);
         if (charAt(str, index) == ',') {
            index = ignoreSpaces(str, ++index);
            index = consumeQuotedText(str, index, elementSep);
            index = ignoreSpaces(str, index);
            if (charAt(str, index) == ',') {
               index = ignoreSpaces(str, ++index);
               index = consumeQuotedText(str, index, keyValueSep);
               index = ignoreSpaces(str, index);
            }
         }

      }
      consume(str, index++, '}');
      if (propertyVariable) {
         tokens.add(new PropertyVariableToken(varName.toString(), initText.toString(), elementSep.toString(), keyValueSep.toString(), testValue.toString(), appendPropertyVal, testEqual, testNotDefined));
      } else {
         tokens.add(new VariableToken(varName.toString(), initText.toString(), elementSep.toString(), keyValueSep.toString()));
      }
      return index;
   }

   private int ignoreSpaces(CharSequence str, int index) {
      for (;;) {
         switch (charAt(str, index)) {
            case ' ':
            case '\n':
            case '\t':
            case '\r':
            case '\f':
               ++index;
               break;
            case '\0':
               return index;
            default:
               return index;
         }
      }
   }

   private char charAt(CharSequence str, int index) {
      if (index >= 0 && index < str.length()) {
         return str.charAt(index);
      }
      return '\0';
   }

   private boolean consume(final CharSequence str, int index, final char charConsume) throws CommandLineTokenizerException {
      if (charAt(str, index) != charConsume) {
         throw new CommandLineTokenizerException("Unexpected character '" + charAt(str, index) + "' at " + index);
      }
      return true;
   }

   private int consumeVariableName(CharSequence str, int index, StringBuilder varName) throws CommandLineTokenizerException {
      char c = charAt(str, index);
      if (Character.isLetter(c)) {
         do {
            varName.append(c);
            c = charAt(str, ++index);
         } while (Character.isLetterOrDigit(c));
      } else {
         throw new CommandLineTokenizerException("Variable name expected at " + index + ". Found '" + c + "'");
      }
      return index;
   }

   private int consumeQuotedText(CharSequence str, int index, StringBuilder text) throws CommandLineTokenizerException {
      if (charAt(str, index) == '\'') {
         final int startIndex = index;
         char c = charAt(str, ++index);
         while (c != '\'' && c != '\0') {
            if (c == '\\') {
               if (charAt(str, index + 1) == '\'') {
                  ++index;
                  text.append('\'');
                  c = charAt(str, ++index);
                  continue;
               }
            }
            text.append(c);
            c = charAt(str, ++index);
         }
         consume(str, index++, '\'');
         if (index - startIndex <= 2) {
            throw new CommandLineTokenizerException("Quoted value cannot be empty.");
         }
      }
      return index;
   }
}
