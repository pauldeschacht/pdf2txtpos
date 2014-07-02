/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pauldeschacht.pdf2txtpos;

import java.util.Comparator;

/**
 *
 * @author pauldeschacht
 */

public class WordPositionComparator implements Comparator<WordPosition> {
    public static float DELTA = 0.00001f;
    
    public int compare(WordPosition wp1, WordPosition wp2) {

        float dy = wp1.y2() - wp2.y2();     //check bottom line
        if(Math.abs(dy) < DELTA) {
            //same line
            float dx = wp1.x1() - wp2.x1();
            if(Math.abs(dx) < DELTA) {
                return 0; //should not happen
            }
            if (dx < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
        else {
            if (dy < 0) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
}
