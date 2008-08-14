package org.basex.query.xpath.expr;

import org.basex.query.FTOpt;
import org.basex.query.QueryException;
import org.basex.query.xpath.XPContext;
import org.basex.query.xpath.values.Bool;
import org.basex.query.xpath.values.FTNode;
import org.basex.util.Array;

/**
 * FTIntersection Expression. 
 * This expresses the intersection of two FTContains results.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Sebastian Gath
 */
public final class FTIntersection extends FTArrayExpr {
  /** Saving index of positive expressions. */
  private int[] pex;
  /** Saving index of negative expressions (FTNot). */
  private int[] nex;
  /** Temp FTNode.  */
  private FTNode nod2;


  
  /**
   * Constructor.
   * @param e operands joined with the union operator
   * @param pexpr IntList with indexes of positive expressions
   * @param nexpr IntList with indexes of negative expressions
   * @Deprecated
   */
  public FTIntersection(final FTArrayExpr[] e, final int[] pexpr, 
      final int[] nexpr) {
    exprs = e;
    pex = pexpr;
    nex = nexpr;
  }

  /**
   * Constructor.
   * @param e operands joined with the union operator
   * @param option FTOption with special features for the evaluation
   * @param pexpr IntList with indexes of positive expressions
   * @param nexpr IntList with indexes of negative expressions
   */
  public FTIntersection(final FTArrayExpr[] e, final FTOpt option, 
      final int[] pexpr, final int[] nexpr) {
    exprs = e;
    fto = option;
    pex = pexpr;
    nex = nexpr;

  }
  
  /**
   * Checks if more values are available.
   * @param n pointer on exprs
   * @return boolean
   */
  private boolean more(final int[] n) {
    for (int i : n) {
      if (!exprs[i].more()) return false;
    }
    return true;
  }
  
  @Override 
  public boolean pos() {
    for (FTArrayExpr i : exprs) {
      if (i.pos()) return true;
    }
    return false;
  }

  
  @Override
  public boolean more() {
    if (pex.length > 0)
      return more(pex);
    return more(nex);

/*    if (init) {
      for (int i : nex) {
        exprs[i].more();
      }
      init = false;
    }
 */   
  }
  
  /**
   * Calculates FTAnd for the node n and the current node.
   * @param n FTNode
   * @param ctx XPContext
   * @return FTNode as resultnode
   */
  private FTNode calcFTAnd(final int[] n, final XPContext ctx) {
    if (n.length == 0) return null;
    else if (n.length == 1) return exprs[n[0]].next(ctx);
    
    FTNode n1 = exprs[n[0]].next(ctx);
    FTNode n2;
    for (int i = 1; i < n.length; i++) {
      n2 = exprs[n[i]].next(ctx);
      if (n1.size() == 0 || n2.size() == 0) return new FTNode();
      int d = n1.getPre() - n2.getPre();
      while(d != 0) {
        if (d < 0) {
          if (i != 1) {
            i = 1;
            n2 = exprs[n[i]].next(ctx);
          }
          if (exprs[n[0]].more())
            n1 = exprs[n[0]].next(ctx);
          else return new FTNode();
        } else {
          if (exprs[n[i]].more())
            n2 = exprs[n[i]].next(ctx);
          else return new FTNode();
        }
        d = n1.getPre() - n2.getPre();
      }
      //if (!n1.merge(n2, 0)) return new FTNode();
    }
    
    for (int i = 1; i < n.length; i++) {
      n2 = exprs[n[i]].next(ctx);
      //n1.merge(n2, i - 1);
      n1.merge(n2, 0);
    }
    return n1;
  }

  @Override
  public FTNode next(final XPContext ctx) {
    FTNode n1 = calcFTAnd(pex, ctx);
    
/*    nod2 = (nex.length > 0 && nod2 == null && more(nex)) ? 
        calcFTAnd(nex, ctx) : nod2;
*/
    if (n1 != null) {
      nod2 = (nex.length > 0 && nod2 == null && more(nex)) ? 
          calcFTAnd(nex, ctx) : nod2;
      if (nod2 != null) {
        int d = n1.getPre() - nod2.getPre();
        while (d > 0) {
          if (!more(nex)) break;
          nod2 = calcFTAnd(nex, ctx);
          if (nod2.size() > 0) {
            d = n1.getPre() - nod2.getPre();
          } else {
            break;
          }
        }
        if (d != 0) return n1;
        else {
          if (more()) {
            nod2 = null;
            return next(ctx);
          } else return new FTNode();
        }
      }
      return n1;
    }
    return calcFTAnd(nex, ctx);
//    return nod2;
  }
  
  
  @Override
  public Bool eval(final XPContext ctx) throws QueryException {
    // check each positive expression
    for (int i : pex) {
      final Bool it = (Bool) exprs[i].eval(ctx);
      if (!it.bool()) return it;
    }
    
    //if (nex.length > 1) {
      for (int i : nex) {
        final Bool it = (Bool) exprs[i].eval(ctx);
        if (!it.bool()) return it;
      }
   // }
    
    return Bool.TRUE;
    
    /*int[][] res = null;
    int[][] tmp;
    int[] pntr = null;
    Object[] o;

    Item it = exprs[0].eval(ctx);
    if(it instanceof NodeSet) {
      if (it instanceof FTNodeSet) {
        FTNodeSet n1 = (FTNodeSet) it;
        it = exprs[1].eval(ctx);
        if (it instanceof FTNodeSet) { 
          FTNodeSet n2 = (FTNodeSet) it;
          FTNodeSet f = calculateFTAnd(n1, n2);
          f.setCTX(ctx);
          ftns = f;
          return f;
        }
      }
      res = ((NodeSet) it).ftidpos;
      pntr = ((NodeSet) it).ftpointer;
      
      for (int i = 1; i < exprs.length; i++) {
        it = exprs[i].eval(ctx);
        if (it instanceof NodeSet) {
          tmp = ((NodeSet) it).ftidpos;
          if (pres) {
            o = calculateFTAndOrderPreserving(res, tmp, pntr); 
            if (o != null && o.length == 2 && o[0] == null && o[1] == null) {
              return new NodeSet(ctx);
            }
            res = (int[][]) o[0];
            pntr = (int[]) o[1];
          } else if(Prop.ftdetails) {
            o = calculateFTAnd(res, tmp, pntr); 
            if (o != null && o.length == 2 && o[0] == null && o[1] == null) {
              return new NodeSet(ctx);
            }
            // <SG> throws ArrayIndexOutOfBound: 0 for some queries;
            // example: input.xml,  //*[text() ftcontains 'X' ftand 'Databases']
            res = (int[][]) o[0];
            pntr = (int[]) o[1];
          } else {
            res = calculateFTAnd(res, tmp);
          }
        }
      }
      if (pres || Prop.ftdetails) {
        return new NodeSet(Array.extractIDsFromData(res), ctx, res, pntr);
      }
      return new NodeSet(Array.extractIDsFromData(res), ctx, res);
    }
    return null;
    */
  }
/*
  public FTNodeSet calculateFTAnd(final FTNodeSet val1, final FTNodeSet val2) {
    final FTNodeSet v1 = (val1.size() < val2.size())? val1 : val2;
    final FTNodeSet v2 = (val1.size() > val2.size())? val1 : val2;
    
    FTNodeSet res = new FTNodeSet(v1.size, false);
    if (v1.more()) {
      if (!v2.more()) return v1;
    } else {
      if (v2.more()) return v2;
      else return null;
    }
    
    while (true) {
      if(v1.curPre() > v2.curPre()) {
        if (!v2.more()) break;
      } else if (v1.curPre() == v2.curPre()) {
        res.addData(val1, val2);
        if (!v1.more() || !v2.more()) break;
      } else {
        if (!v1.more()) break;
      }
    }
    
    while (v1.more()) res.addData(v1);
    while (v2.more()) res.addData(v2);
    
    res.finish();
    
    return res;
  }
  */
  /**
   * Built join for value1 and value2; used key is id.
   * @param val1 input set int[][]
   * @param val2 input set  int[][]
   * @return result int[][]
   */
  public int[][] calculateFTAnd(final int[][] val1, final int[][] val2) {
    int lastId = -1;
    int[][] values1 = val1;
    int[][] values2 = val2;

    if(values1 == null || values1[0].length == 0 || values2 == null
        || values2[0].length == 0) {
      return new int[][]{};
    }

    // calculate minimum size
    int min = Math.min(values1[0].length, values2[0].length);
    // double space, because 2 values for each identical id
    int[][] maxResult = new int[2][values1[0].length + values2[0].length];

    //if (min == values2.length && min != values1.length) {
    if(min == values2[0].length && min != values1[0].length) {

      // change arrays
      int[][] tmp = values2;
      values2 = values1;
      values1 = tmp;
      //changedOrder = true;
    }

    // run variable for values1
    int i = 0;
    // run variable for values2
    int k = 0;
    // count added elements
    int counter = 0;
    int cmpResult;

    // each value from the smaller set are compared with the bigger set and
    // added to result
    while(i < values1[0].length && k < values2[0].length) {
      cmpResult = Array.compareIntArrayEntry(values1[0][i], values1[1][i],
          values2[0][k], values2[1][k]);
      if(cmpResult == -1) {
        // same Id, but pos1 < pos 2 values1[i] < values2[k]
        maxResult[0][counter] = values1[0][i];
        maxResult[1][counter] = values1[1][i];

        lastId = values1[0][i];
        counter++;
        i++;
      } else if(cmpResult == -2) {
        // id1 < i2
        if(lastId != values1[0][i]) {
          i++;
        } else {
          // same value and Id == lastId have to be copied
          while(i < values1[0].length && lastId == values1[0][i]) {

            maxResult[0][counter] = values1[0][i];
            maxResult[1][counter] = values1[1][i];

            counter++;
            i++;
          }
        }
      } else if(cmpResult == 2) {
        // id1 > i2
        if(lastId != values2[0][k]) {

          k++;
        } else {
          // all values with same Id == lastId have to be copied
          while(k < values2[0].length && lastId == values2[0][k]) {

            maxResult[0][counter] = values2[0][k];
            maxResult[1][counter] = values2[1][k];

            counter++;
            k++;
          }
        }
      } else if(cmpResult == 1) {
        // same ids, but pos1 > pos2 values1[i] > values2[k]
        maxResult[0][counter] = values2[0][k];
        maxResult[1][counter] = values2[1][k];

        lastId = values2[0][k];
        counter++;
        k++;
      } else {
        // entry identical
        maxResult[0][counter] = values2[0][k];
        maxResult[1][counter] = values2[1][k];
        counter++;
        i++;
        k++;
      }
    }

    // process left elements form values1, values2 done
    while(k > 0 && values1[0].length > i &&
        values1[0][i] == values2[0][k - 1]) {
      maxResult[0][counter] = values1[0][i];
      maxResult[1][counter] = values1[1][i];
      counter++;
      i++;
    }

    // process left elements form values2, values1 done
    while(i > 0 && values2[0].length > k &&
        values2[0][k] == values1[0][i - 1]) {
      //maxResult[counter] = values2[k];
      maxResult[0][counter] = values2[0][k];
      maxResult[1][counter] = values2[1][k];
      counter++;
      k++;
    }

    if(counter == 0) return new int[][]{};

    int[][] returnArray = new int[2][counter];
    System.arraycopy(maxResult[0], 0, returnArray[0], 0, counter);
    System.arraycopy(maxResult[1], 0, returnArray[1], 0, counter);

    return returnArray;
  }

  /**
   * Built join for value1 and value2. 
   * The resultset has the same
   * order, as the searchvalues are written in the query.
   * The variable pointer saves for each id the original position in the query;
   * it is updated each time
   *
   * @param val2 inputset int[][]
   * @param val1 inputset  int[][]
   * @param po pointer int[]
   * @return Object[] o[0]=int[][] results; o[1]=pointers
   */

  public Object[] calculateFTAndOrderPreserving(final int[][] val2, 
      final int[][]val1, final int[] po) {
    
    if (val1 == null || val1[0].length == 0 || val2 == null
        || val2[0].length == 0) {
      return new Object[]{null, null};
    } 
    
    int[][] v1 = val1;
    int[][] v2 = val2;
    int[] p = po;
    
    int min = Math.min(v1[0].length, v2[0].length);
    // note changed order
    boolean changedOrder = false;

    if (min == v2[0].length && min != v1[0].length) {
      // change arrays
      int[][] tmp = v2;
      v2 = v1;
      v1 = tmp;
      changedOrder = true;
    }

    int[][] maxResult = new int[2][v1[0].length + v2[0].length];
    // space for new pointer
    int[] pointersnew = new int[maxResult[0].length + 1];

    // first call FTAND
    if (p == null) {
      p = new int[pointersnew.length];
    }

    // first element in pointers shows the maximum level
    pointersnew[0] = p[0] + 1;

    // run variable for values1
    int i = 0;
    // run variable for values2
    int k = 0;
    // number inserted elements
    int counter = 0;

    // each value from the smaller set are compared with the bigger set and
    // added to result
    while(v1[0].length > i) {
      if (k == v2[0].length) {
        break;
      }

      // same Ids
      if (v2[0][k] == v1[0][i]) {
        // changed order
        if (!changedOrder) {
          // copy values2
          while (k < v2[0].length 
              && v2[0][k] == v1[0][i]) {
            // same id
            //maxResult[counter] = values2[k];
            maxResult[0][counter] = v2[0][k];
            maxResult[1][counter] = v2[1][k];
            // copy old pointer
            pointersnew[counter + 1] = p[k + 1];
            counter++;
            k++;
          }
          // copy values1
          while (i < v1[0].length 
              && v2[0][k - 1] == v1[0][i]) {
            // same ids
            maxResult[0][counter] = v1[0][i];
            maxResult[1][counter] = v1[1][i];
            // add new element
            pointersnew[counter + 1] = pointersnew[0];
            counter++;
            i++;
          }
        } else {
          // copy values1
          while (i < v1[0].length 
              && v2[0][k] == v1[0][i]) {
            // same ids
            maxResult[0][counter] = v1[0][i];
            maxResult[1][counter] = v1[1][i];
            // add new element
            pointersnew[counter + 1] = p[i + 1]; //k
            counter++;
            i++;
          }
          // copy values2
          while (k < v2[0].length 
              && v2[0][k] == v1[0][i - 1]) {
            // same ids
            maxResult[0][counter] = v2[0][k];
            maxResult[1][counter] = v2[1][k];

            // copy old pointer
            pointersnew[counter + 1] = pointersnew[0];
            counter++;
            k++;
          }
        }
      } else if (v1[0][i] < v2[0][k]) {
        i++;
      } else {
        k++;
      }
    }

    // process left elements form values1, values2 done
    while(k > 0 && v1[0].length > i 
        && v1[0][i] == v2[0][k - 1]) {
      //maxResult[counter] = values1[i];
      maxResult[0][counter] = v1[0][i];
      maxResult[1][counter] = v1[1][i];
      // new element
      if (!changedOrder) {
        pointersnew[counter + 1] = pointersnew[0];
      } else {
        pointersnew[counter + 1] = p[k + 1];
      }

      counter++;
      i++;
    }


    // process left elements form values2, values1 done
    while(i > 0 && v2[0].length > k 
        && v2[0][k] == v1[0][i - 1]) {
      maxResult[0][counter] = v2[0][k];
      maxResult[1][counter] = v2[1][k];
      // copy old pointer
      if (!changedOrder) {
        pointersnew[counter + 1] = p[k + 1];
      } else {
        pointersnew[counter + 1] = pointersnew[0];
      }

      counter++;
      k++;
    }

    if (counter == 0) return new Object[]{null, null};

    int[][] rnArray = new int[2][counter];
    System.arraycopy(maxResult[0], 0, rnArray[0], 0, counter);
    System.arraycopy(maxResult[1], 0, rnArray[1], 0, counter);
    p = new int[counter + 1];
    System.arraycopy(pointersnew, 0, p, 0, counter + 1);

    Object[] o = new Object[2];
    o[0] = rnArray;
    o[1] = p;

    return o;
  }


  /**
   * Built join for value1 and value2; used key is id. 
   * Servers pointer on search strings for each id.
   * @param val1 input set int[][]
   * @param val2 input set  int[][]
   * @param p int[] pointer array, optional on val1
   * @return result int[][]
   */
  public Object[] calculateFTAnd(final int[][] val1, final int[][] val2, 
      final int[] p) {
    int lastId = -1;
    int[][] values1 = val1;
    int[][] values2 = val2;
    
    if(values1 == null || values1[0].length == 0 || values2 == null
        || values2[0].length == 0) {
      return new Object[]{null, null};
    }

    int[] pn = new int[val1[0].length + val2[0].length + 1];
    if(p != null) pn[0] = p[0] + 1;
    else pn[0] = 1;
    
    // calculate minimum size
    int min = Math.min(values1[0].length, values2[0].length);
    // double space, because 2 values for each identical id
    int[][] maxResult = new int[2][values1[0].length + values2[0].length];
    boolean co = false;
    
    if(min == values2[0].length && min != values1[0].length) {
      // change arrays
      int[][] tmp = values2;
      values2 = values1;
      values1 = tmp;
      co = true;
    }

    // run variable for values1
    int i = 0;
    // run variable for values2
    int k = 0;
    // count added elements
    int counter = 0;
    int cmpResult;

    // each value from the smaller set are compared with the bigger set and
    // added to result
    while(i < values1[0].length && k < values2[0].length) {
      cmpResult = Array.compareIntArrayEntry(values1[0][i], values1[1][i],
          values2[0][k], values2[1][k]);
      if(cmpResult == -1) {
        // same Id, but pos1 < pos 2 values1[i] < values2[k]
        maxResult[0][counter] = values1[0][i];
        maxResult[1][counter] = values1[1][i];

        // copy old pointer
        pn[counter + 1] = co ? pn[0] : p != null ? p[i + 1] : 0;

        lastId = values1[0][i];
        counter++;

        i++;
      } else if(cmpResult == -2) {
        // id1 < i2
        if(lastId != values1[0][i]) {
          i++;
        } else {
          // same value and Id == lastId have to be copied
          while(i < values1[0].length && lastId == values1[0][i]) {

            maxResult[0][counter] = values1[0][i];
            maxResult[1][counter] = values1[1][i];

            // copy old pointer
            pn[counter + 1] = co ? pn[0] : p != null ? p[i + 1] : 0;

            counter++;
            i++;
          }
        }
      } else if(cmpResult == 2) {
        // id1 > i2
        if(lastId != values2[0][k]) {

          k++;
        } else {
          // all values with same Id == lastId have to be copied
          while(k < values2[0].length && lastId == values2[0][k]) {

            maxResult[0][counter] = values2[0][k];
            maxResult[1][counter] = values2[1][k];

            // copy old pointer
            pn[counter + 1] = !co ? pn[0] : p != null ? p[k + 1] : 0;
            
            counter++;
            k++;
          }
        }
      } else if(cmpResult == 1) {
        // same ids, but pos1 > pos2 values1[i] > values2[k]
        maxResult[0][counter] = values2[0][k];
        maxResult[1][counter] = values2[1][k];

        // copy old pointer
        pn[counter + 1] = !co ? pn[0] : p != null ? p[k + 1] : 0;
        
        lastId = values2[0][k];
        counter++;
        k++;
      } else {
        // entry identical
        maxResult[0][counter] = values2[0][k];
        maxResult[1][counter] = values2[1][k];
        counter++;
        // copy old pointer
        pn[counter + 1] = !co ? pn[0] : p != null ? p[k + 1] : 0;

        i++;
        k++;
      }
    }

    // process left elements form values1, values2 done
    while(k > 0 && values1[0].length > i &&
        values1[0][i] == values2[0][k - 1]) {
      maxResult[0][counter] = values1[0][i];
      maxResult[1][counter] = values1[1][i];
      
      // copy old pointer
      pn[counter + 1] = co ? pn[0] : p != null ? p[i + 1] : 0;
      
      counter++;
      i++;
    }

    // process left elements form values2, values1 done
    while(i > 0 && values2[0].length > k &&
        values2[0][k] == values1[0][i - 1]) {
      //maxResult[counter] = values2[k];
      maxResult[0][counter] = values2[0][k];
      maxResult[1][counter] = values2[1][k];
      // copy old pointer
      pn[counter + 1] = !co ? pn[0] : p != null ? p[k + 1] : 0;

      counter++;
      k++;
    }

    if(counter == 0) return new int[][]{};

    int[][] returnArray = new int[2][counter];
    System.arraycopy(maxResult[0], 0, returnArray[0], 0, counter);
    System.arraycopy(maxResult[1], 0, returnArray[1], 0, counter);
    
    int[] poi = new int[counter + 1];
    System.arraycopy(pn, 0, poi, 0, counter + 1);
    
    return new Object[]{returnArray, poi};
  }

 
}
