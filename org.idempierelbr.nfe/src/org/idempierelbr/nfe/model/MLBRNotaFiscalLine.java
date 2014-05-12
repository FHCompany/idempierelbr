/******************************************************************************
 * Product: ADempiereLBR - ADempiere Localization Brazil                      *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package org.idempierelbr.nfe.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MProduct;
import org.compiere.model.MTax;
import org.compiere.model.MTaxProvider;
import org.compiere.util.Env;

/**
 *	MNotaFiscalLine
 *
 *	Model for X_LBR_NotaFiscalLine
 */
public class MLBRNotaFiscalLine extends X_LBR_NotaFiscalLine {

	/**
	 *	Serial
	 */
	private static final long serialVersionUID = 1L;

	/** Parent					*/
	private MLBRNotaFiscal			m_parent = null;
	
	/**	Process Message */
	private String		m_processMsg = null;
	
	/** Cached Currency Precision	*/
	private Integer			m_precision = null;
	
	/**	Product					*/
	private MProduct 		m_product = null;

	public String getProcessMsg() {

		if (m_processMsg == null)
			m_processMsg = "";

		return m_processMsg;
	}

	/**************************************************************************
	 *  Default Constructor
	 *  @param Properties ctx
	 *  @param int ID (0 create new)
	 *  @param String trx
	 */
	public MLBRNotaFiscalLine (Properties ctx, int ID, String trx)
	{
		super(ctx, ID, trx);
	}	//	MLBRNotaFiscalLine

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *  @param trxName transaction
	 */
	public MLBRNotaFiscalLine (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MLBRNotaFiscalLine
	
	/**
	 *  Constructor
	 *  @param nf Nota Fiscal
	 */
	public MLBRNotaFiscalLine (MLBRNotaFiscal nf)
	{
		super(nf.getCtx(), 0, nf.get_TrxName());
		setLBR_NotaFiscal_ID(nf.getLBR_NotaFiscal_ID());
		//
		m_parent = nf;
	}	//	MLBRNotaFiscalLine
	
	/**
	 * 	Set Header Info
	 *	@param nf nf
	 */
	public void setHeaderInfo (MLBRNotaFiscal nf)
	{
		m_parent = nf;
		m_precision = new Integer(nf.getPrecision());
	}	//	setHeaderInfo
	
	/**
	 * 	Necess�rio para ajustar a precis�o
	 * 		de casas decimais
	 */
	public void setPriceActual (BigDecimal Price)
	{
		if (Price == null)
			Price = Env.ZERO;
		//
		super.setPriceActual(Price.setScale(10, BigDecimal.ROUND_HALF_UP));
	}	//	setPrice
	
	/**
	 * 	Necess�rio para ajustar a precis�o
	 * 		de casas decimais
	 */
	public void setQty (BigDecimal Qty)
	{
		if (Qty == null)
			Qty = Env.ZERO;
		//
		super.setQty(Qty.setScale(4, BigDecimal.ROUND_HALF_UP));
	}	//	setQty
	
	/**
	 * 	Get Parent
	 *	@return parent
	 */
	public MLBRNotaFiscal getParent()
	{
		if (m_parent == null)
			m_parent = new MLBRNotaFiscal (getCtx(), getLBR_NotaFiscal_ID(), get_TrxName());
		return m_parent;
	}	//	getParent
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return saved
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (!success)
			return success;
		MTax tax = new MTax(getCtx(), getC_Tax_ID(), get_TrxName());
        MTaxProvider provider = new MTaxProvider(tax.getCtx(), tax.getC_TaxProvider_ID(), tax.get_TrxName());
		ITaxProviderNfe calculator = new NFTaxProvider();
    	return calculator.recalculateTax(provider, this, newRecord);
	}	//	afterSave
	
	/**
	 * Recalculate NF tax
	 * @param oldTax true if the old C_Tax_ID should be used
	 * @return true if success, false otherwise
	 * 
	 * @author teo_sarca [ 1583825 ]
	 */
	public boolean updateNFTax(boolean oldTax) {
		MLBRNotaFiscalTax tax = MLBRNotaFiscalTax.get (this, getParent().getPrecision(), oldTax, get_TrxName());
		if (tax != null) {
			if (!tax.calculateTaxFromLines())
				return false;
			if (tax.getTaxAmt().signum() != 0) {
				if (!tax.save(get_TrxName()))
					return false;
			}
			else {
				if (!tax.is_new() && !tax.delete(false, get_TrxName()))
					return false;
			}
		}
		return true;
	}
	
	/**
	 *	Update Tax & Header
	 *	@return true if header updated
	 */
	public boolean updateHeaderTax()
	{

		// Update header only if the document is not processed
		if (isProcessed() && !is_ValueChanged(COLUMNNAME_Processed))
			return true;

		MTax tax = new MTax(getCtx(), getC_Tax_ID(), get_TrxName());
        MTaxProvider provider = new MTaxProvider(tax.getCtx(), tax.getC_TaxProvider_ID(), tax.get_TrxName());
        ITaxProviderNfe calculator = new NFTaxProvider();
        if (!calculator.updateNFTax(provider, this))
			return false;

    	return calculator.updateHeaderTax(provider, this);
	}	//	updateHeaderTax
	
	public void clearParent()
	{
		this.m_parent = null;
	}
	
	/**
	 *	Is Tax Included in Amount
	 *	@return true if tax calculated
	 */
	public boolean isTaxIncluded()
	{
		return true;
	}	//	isTaxIncluded
	
	/**
	 * 	Get Product
	 *	@return product or null
	 */
	public MProduct getProduct()
	{
		if (m_product == null && getM_Product_ID() != 0)
			m_product =  MProduct.get (getCtx(), getM_Product_ID());
		return m_product;
	}	//	getProduct
}
