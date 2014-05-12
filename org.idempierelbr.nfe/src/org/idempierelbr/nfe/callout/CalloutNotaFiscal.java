package org.idempierelbr.nfe.callout;

import java.math.BigDecimal;
import java.util.Properties;
import org.adempiere.base.IColumnCallout;
import org.adempiere.model.POWrapper;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MCurrency;
import org.compiere.util.Env;
import org.idempierelbr.nfe.model.MLBRNotaFiscal;
import org.idempierelbr.nfe.model.MLBRNotaFiscalLine;
import org.idempierelbr.tax.wrapper.I_W_C_BPartner;

public class CalloutNotaFiscal implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		
		if (mTab.getTableName().equals(MLBRNotaFiscal.Table_Name))
			if (mField.getColumnName().equals(MLBRNotaFiscal.COLUMNNAME_C_BPartner_ID))
				return setTransactionType(ctx, mTab, value);
			else 
				return null;
		else if (mTab.getTableName().equals(MLBRNotaFiscalLine.Table_Name))
			if (mField.getColumnName().equals(MLBRNotaFiscalLine.COLUMNNAME_Qty) ||
					mField.getColumnName().equals(MLBRNotaFiscalLine.COLUMNNAME_PriceActual))
				return setLineNetAmount(ctx, mTab, value);
			else 
				return null;
		else
			return null;
	}
	
	/**
	 * Define o Tipo de Transa��o com base no Parceiro de Neg�cios
	 */
	private String setTransactionType(Properties ctx, GridTab mTab, Object value) {
		Integer C_BPartner_ID = (Integer) value;
		
		if (C_BPartner_ID == null || C_BPartner_ID == 0) {
			mTab.setValue("LBR_TransactionType", null);
			return "";
		}
		
		MBPartner bp = new MBPartner(ctx, C_BPartner_ID, null);
		I_W_C_BPartner bpW = POWrapper.create(bp, I_W_C_BPartner.class);
		
		if (Env.isSOTrx(ctx, mTab.getWindowNo()))
			mTab.setValue("LBR_TransactionType", bpW.getLBR_TransactionType_Customer());
		else
			mTab.setValue("LBR_TransactionType", bpW.getLBR_TransactionType_Vendor());
		
		return "";
	}
	
	/**
	 * Define o Tipo de Transa��o com base no Parceiro de Neg�cios
	 */
	private String setLineNetAmount(Properties ctx, GridTab mTab, Object value) {
		BigDecimal qty = (BigDecimal)mTab.getValue("Qty");
		BigDecimal priceActual = (BigDecimal)mTab.getValue("PriceActual");
		int stdPrecision = MCurrency.getStdPrecision(ctx, MLBRNotaFiscal.CURRENCY_BRL);
		
		if (qty != null && priceActual != null) {
			BigDecimal LineNetAmt = qty.multiply(priceActual);
			if (LineNetAmt.scale() > stdPrecision)
				LineNetAmt = LineNetAmt.setScale(stdPrecision, BigDecimal.ROUND_HALF_UP);
			mTab.setValue("LineNetAmt", LineNetAmt);
		}
		
		return "";
	}
}