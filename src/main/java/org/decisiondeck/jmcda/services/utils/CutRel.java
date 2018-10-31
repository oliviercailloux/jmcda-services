package org.decisiondeck.jmcda.services.utils;

import static com.google.common.base.Preconditions.checkArgument;

import org.decision_deck.utils.matrix.SparseMatrixDRead;
import org.decision_deck.utils.matrix.SparseMatrixFuzzy;
import org.decision_deck.utils.matrix.mess.ZeroToOneMatrix;

/**
 * Cuts a matrix at some specified threshold and transforms it into a binary matrix.
 * 
 * @author Olivier Cailloux
 * 
 * @param <RowType>
 *            the row type.
 * @param <ColumnType>
 *            the column type.
 */
public class CutRel<RowType, ColumnType> {
    /**
     * Cuts the given matrix at the specified threshold and returns a binary matrix containing the same number of values
     * as the input one. Any value in the input matrix greater than or equal to the threshold will transform to a one in
     * the output matrix. Other values in the input matrix are transformed to a zero in the output matrix.
     * 
     * @param toCut
     *            not <code>null</code>, may be incomplete.
     * @param threshold
     *            may be infinite, not NAN.
     * @return not <code>null</code>, may be empty.
     */
    public SparseMatrixFuzzy<RowType, ColumnType> cut(SparseMatrixDRead<RowType, ColumnType> toCut, double threshold) {
	checkArgument(!Double.isNaN(threshold));

	final SparseMatrixFuzzy<RowType, ColumnType> out = new ZeroToOneMatrix<RowType, ColumnType>();
	for (RowType rowAlt : toCut.getRows()) {
	    for (ColumnType colAlt : toCut.getColumns()) {
		final Double entry = toCut.getEntry(rowAlt, colAlt);
		if (entry == null) {
		    continue;
		}
		final double value = entry.doubleValue();
		out.put(rowAlt, colAlt, (value >= threshold) ? 1d : 0d);
	    }
	}

	return out;
    }
}
