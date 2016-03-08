/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.com.uggeri.build.tools.custom.tokenizer;

/**
 *
 * @author Fabio
 */
public class TextToken implements CommandLineToken {
   
   private String text;

   public TextToken(CharSequence text) {
      this.text = text.toString();
   }

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = text;
   }

   @Override
   public String toString() {
      return text;
   }
}
