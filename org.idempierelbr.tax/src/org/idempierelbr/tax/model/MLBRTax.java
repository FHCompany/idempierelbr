package org.idempierelbr.tax.model;

/******************************************************************************
 * Copyright (C) 2011 Kenos Assessoria e Consultoria de Sistemas Ltda         *
 * Copyright (C) 2011 Ricardo Santana                                         *
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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.model.POWrapper;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempierelbr.tax.model.MLBRNCM;
import org.idempierelbr.tax.wrapper.I_W_C_BPartner;
import org.idempierelbr.tax.wrapper.I_W_LBR_NCM;
import org.idempierelbr.tax.wrapper.I_W_M_Product;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * 		Model for MLBRTax
 * 
 * 	@author Ricardo Santana (Kenos, www.kenos.com.br)
 * 			<li> Sponsored by Soliton, www.soliton.com.br
 *	@version $Id: MLBRTax.java, v1.0 2011/05/05 8:19:03 PM, ralexsander Exp $
 *		
 *		Old Version:
 *	@contributor Mario Grigioni
 *  @contributor Fernando Lucktemberg (Faire, www.faire.com.br)
 */
public class MLBRTax extends X_LBR_Tax 
{
	/** Serial			*/
	private static final long serialVersionUID = 1932340299220283663L;
	
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(MLBRTax.class);

	/**	Numereals		*/
	private static final BigDecimal ONE 		= Env.ONE.setScale(17, BigDecimal.ROUND_HALF_UP);
	private static final BigDecimal ONEHUNDRED 	= Env.ONEHUNDRED.setScale(17, BigDecimal.ROUND_HALF_UP);
	
	/**	SISCOMEX		*/
	public static final String SISCOMEX 	= "SISCOMEX";
	
	/**	Freight			*/
	public static final String FREIGHT 		= "FREIGHT";
	
	/**	OTHERCHARGES		*/
	public static final String OTHERCHARGES = "OTHERCHARGES";
	
	/**	Insurance		*/
	public static final String INSURANCE 	= "INSURANCE";
	
	/**	Amount			*/
	public static final String AMT 			= "AMT";
	
	/**	Qty			*/
	public static final String QTY 			= "QTY";
	
	/**	Calculation Type	*/
	public static final int TYPE_RATE_OR_IVA 	= 100;
	public static final int TYPE_TARIFF 		= 101;
	public static final int TYPE_LIST_MAX 		= 102;
	public static final int TYPE_AMOUNT 		= 103;
	public static final int TYPE_LIST_POSITIVE 	= 104;
	public static final int TYPE_LIST_NEUTRAL 	= 105;
	public static final int TYPE_LIST_NEGATIVE 	= 106;
	
	/**	ST				*/
	private static boolean hasSubstitution    	= false;
	
	/**	Included Taxes	*/
	private List<Integer> includedTaxes = new ArrayList<Integer>();

	/**************************************************************************
	 *  Default Constructor
	 *  @param Properties ctx
	 *  @param int LBR_Tax_ID (0 create new)
	 *  @param String trx
	 */
	public MLBRTax (Properties ctx, int LBR_Tax_ID, String trx)
	{
		super (ctx, LBR_Tax_ID, trx);
	}	//	MLBRTax

	/**
	 *  Load Constructor
	 *  @param ctx context
	 *  @param rs result set record
	 *  @param trxName transaction
	 */
	public MLBRTax (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MLBRTax
	
	/**
	 * 	Calculate taxes
	 * @param amt
	 * @param isTaxIncludedPriceList
	 * @param trxType
	 */
	public void calculate (boolean isTaxIncludedPriceList, Timestamp dateDoc, Map<String, BigDecimal> params, String trxType)
	{
		MLBRTaxLine[] taxLines = getLines();
		
		/**
		 * 	Os impostos de ST devem ser calculados por �ltimo
		 */
		Arrays.sort (taxLines, new Comparator<MLBRTaxLine>()
		{
			public int compare (MLBRTaxLine tl1, MLBRTaxLine tl2)
			{
				if (MLBRTaxName.LBR_TAXTYPE_Substitution.equals(tl1.getLBR_TaxName().getLBR_TaxType()))
					return 1;
				return -1;
			}	//	compare
		});
		
		/**
		 * 	Faz o c�lculo para todos os impostos
		 */
		for (MLBRTaxLine taxLine : taxLines)
		{
			MLBRTaxName taxName = new MLBRTaxName (Env.getCtx(), taxLine.getLBR_TaxName_ID(), null);
			MLBRTaxFormula taxFormula = taxName.getFormula (trxType, dateDoc);
			//
			log.fine("[MLBRTaxName=" + taxName.getName() + ", MLBRTaxFormula=" + taxFormula + "]");
			//
			BigDecimal taxBase		= Env.ZERO;
			BigDecimal taxBaseAdd 	= Env.ZERO;
			BigDecimal taxAmt		= Env.ZERO;
			BigDecimal amountBase 	= Env.ZERO;
			BigDecimal factor 		= Env.ONE;
			int calculationType = getCalculationType(taxLine);
			
			/**
			 * 		Valor da Opera��o
			 * 	Apenas o valor dos produtos, sem c�lculos adicionais
			 */
			if (calculationType == TYPE_AMOUNT)
			{
				taxBase = params.get(AMT).setScale(17, BigDecimal.ROUND_HALF_UP);
				taxAmt = getTaxAmt (taxBase, taxLine.getLBR_TaxRate(), false);
			}
			
			/**
			 * 		Valor de Pauta
			 * 	Valor do imposto definido, multiplicado pela quantidade. Ex. S�lo do IPI
			 */
			else if (calculationType == TYPE_TARIFF)
			{
				taxLine.setQty(params.get(QTY));
				//
//				taxBase = params.get(AMT).setScale(17, BigDecimal.ROUND_HALF_UP);
				taxAmt = getTaxAmt (taxLine.getLBR_TaxListAmt(), params.get(QTY), true);
			}
			
			/**
			 * 		Valor de Lista ou M�ximo
			 * 	Valor da BC definida, multiplicado pela quantidade, multiplicado pela al�quota. Ex. Substirui��o por Valor Fixo
			 */
			else if (calculationType == TYPE_LIST_MAX)
			{
				taxLine.setQty(params.get(QTY));
				//
				taxBase = taxLine.getLBR_TaxListAmt().multiply(params.get(QTY)).add(getIncludedAmt(taxLines).add(params.get(FREIGHT)));
				taxAmt = getTaxAmt (taxBase, taxLine.getLBR_TaxRate(), false);
			}
			
			/**
			 * 		C�lculo por Al�quota ou Margem de Valor Agregado
			 */
			else if (calculationType == TYPE_RATE_OR_IVA)
			{
				//	Calcular por f�rmula
				if (taxFormula != null)
				{
					//	Fator do imposto
					factor 		= evalFormula (taxFormula.getFormula(isTaxIncludedPriceList), params);
					
					//	Valores adicionais para a BC
					if (taxFormula.getLBR_FormulaAdd_ID() > 0)
						taxBaseAdd = evalFormula (taxFormula.getLBR_FormulaAdd().getLBR_Formula(), params).setScale(17, BigDecimal.ROUND_HALF_UP);
					
					//	Valor base para �nicio do c�lculo
					if (taxFormula.getLBR_FormulaBase_ID() > 0)
						amountBase = evalFormula (taxFormula.getLBR_FormulaBase().getLBR_Formula(), params).setScale(17, BigDecimal.ROUND_HALF_UP);
					
					//	Caso n�o tenha sido parametrizado, utilizar apenas o valor do documento
					else
						amountBase = params.get(AMT).setScale(17, BigDecimal.ROUND_HALF_UP);
					
					//	Marca se o imposto est� incluso no pre�o
					taxLine.setIsTaxIncluded(taxFormula.isTaxIncluded());
				}
				
				//	Caso n�o tenha uma f�rmula atribuida, considerar o flag da Lista de Pre�os
				else
					taxLine.setIsTaxIncluded(isTaxIncludedPriceList);
				
				/****************************************
				 *  	 	 Adicional x Fator			*
				 *   Base = -------------------			*
				 *  		1 - (Red. BC / 100)			*
				 ***************************************/
				taxBase = taxBaseAdd.add(factor.multiply(amountBase))
						.multiply(ONE.subtract(taxLine.getLBR_TaxBase().setScale(17, BigDecimal.ROUND_HALF_UP).divide(ONEHUNDRED, 17, BigDecimal.ROUND_HALF_UP))).setScale(2, BigDecimal.ROUND_HALF_UP);
				
				taxAmt = getTaxAmt (taxBase, taxLine.getLBR_TaxRate(), false);
			}
			
			//	Encontra o valor previamente calculado para ST
			if (MLBRTaxName.LBR_TAXTYPE_Substitution.equals(taxName.getLBR_TaxType())
					&& taxName.getLBR_TaxSubstitution_ID() > 0)
			{
				for (MLBRTaxLine taxLineSubs : taxLines)
				{
					//	Calcula a diferen�a do imposto
					if (taxLineSubs.getLBR_TaxName_ID() == taxName.getLBR_TaxSubstitution_ID())
					{
						taxAmt = taxAmt.subtract (taxLineSubs.getLBR_TaxAmt());
						break;
					}
				}
			}
			
			//	Inverte o valor dos impostos para os casos de reten��o
			if (MLBRTaxName.LBR_TAXTYPE_Service.equals(taxName.getLBR_TaxType())
					&& taxName.isLBR_HasWithHold())
				taxAmt = taxAmt.negate();
			
			//	N�o postar
//			if (!taxLine.islbr_PostTax())
//			{
//				taxBase = Env.ZERO;
//				taxAmt 	= Env.ZERO;
//			}
			//
			taxLine.setLBR_TaxBaseAmt(taxBase);
			taxLine.setLBR_TaxAmt(taxAmt);
			taxLine.save();
		}
	}	//	calculate

	/**
	 * 	Calculate Tax Amount
	 * 
	 * 	@param taxBase
	 * 	@param taxRate
	 * 	@return
	 */
	private BigDecimal getIncludedAmt (MLBRTaxLine[] taxLines)
	{
		BigDecimal included = Env.ZERO;
		//
		for (MLBRTaxLine taxLineSubs : taxLines)
		{
			//	Calcula a diferen�a do imposto
			if (includedTaxes.contains(taxLineSubs.getLBR_TaxName_ID()))
			{
				included = included.add (taxLineSubs.getLBR_TaxAmt());
				break;
			}
		}
		//
		return included;
	}	//	getTaxAmt
	
	/**
	 * 	Calculate Tax Amount
	 * 
	 * 	@param taxBase
	 * 	@param taxRate
	 * 	@return
	 */
	private BigDecimal getTaxAmt (BigDecimal taxBase, BigDecimal taxRateOrQty, boolean isQty)
	{
		if (taxBase == null || taxBase.signum() == 0 || taxRateOrQty == null || taxRateOrQty.signum() == 0)
			return Env.ZERO;
		//
		if (isQty)
			return taxBase.multiply(taxRateOrQty.setScale(17, BigDecimal.ROUND_HALF_UP))
																 .setScale(2, BigDecimal.ROUND_HALF_UP);
		else
			return taxBase.multiply(taxRateOrQty.setScale(17, BigDecimal.ROUND_HALF_UP))
				.divide(ONEHUNDRED, 17, BigDecimal.ROUND_HALF_UP).setScale(2, BigDecimal.ROUND_HALF_UP);
	}	//	getTaxAmt

	/**
	 * 	Get tax factor
	 * @param formula
	 * @return factor
	 */
	public BigDecimal evalFormula (String formula)
	{
		return evalFormula (formula, null);
	}	//	evalFormula
	
	/**
	 * 	Get tax factor
	 * @param formula
	 * @param params
	 * @return factor
	 */
	public BigDecimal evalFormula (String formula, Map<String, BigDecimal> params)
	{
		if (formula == null || formula.length() == 0)
			return Env.ONE;
		//
		Interpreter bsh = new Interpreter ();
		BigDecimal result = Env.ZERO;
		//
		try
		{
			log.finer ("Formula=" + formula);
			
			/**
			 * 	Permite recursividade nas f�rmulas
			 */
			formula = MLBRFormula.parseFormulas (formula);
			
			/**
			 * 	Assim o erro de divis�o por ZERO � evitado
			 * 		ent�o n�o implica em ter que criar uma f�rmula nova
			 * 		para casos onde alguma al�quota espec�fica � zero.
			 */
			for (MLBRTaxName txName : MLBRTaxName.getTaxNames())
				if (formula.indexOf(txName.getName().trim()) > 0)
				{
					log.finer ("Fill to ZERO, TaxName=" + txName.getName().trim() + "=0");
					bsh.set(txName.getName().trim(), 1/Math.pow (10, 17));
				}
			
			//	Ajusta as al�quotas
			for (MLBRTaxLine tLine : getLines())
			{
				Double amt = tLine.getLBR_TaxRate().setScale(17, BigDecimal.ROUND_HALF_UP)
						.divide(Env.ONEHUNDRED, BigDecimal.ROUND_HALF_UP).doubleValue();
				//
				log.finer ("Set Tax Rate, TaxName=" + tLine.getLBR_TaxName().getName().trim() + "=" + amt);
				bsh.set(tLine.getLBR_TaxName().getName().trim(), amt);
			}
			//	Ajusta os par�metros opcionais (ex. Frete, SISCOMEX)
			if (params != null) for (String key : params.keySet())
			{				
				log.finer ("Set Parameters, Parameter=" + key + "=" + params.get(key).doubleValue());
				bsh.set(key, params.get(key).doubleValue());
			}
			//
			result = new BigDecimal ((Double) bsh.eval(formula));
		}
		catch (EvalError e)
		{
			e.printStackTrace();
		}
		
		return result;
	}	//	evalFormula
	
	/**
	 * 		Eval the Script to find out the calculation type
	 * 	@param line
	 * 	@return
	 */
	private int getCalculationType (MLBRTaxLine line)
	{
		if (line == null || line.getLBR_TaxBaseType_ID() < 1)
			return TYPE_RATE_OR_IVA;	//	Default Value
		
		String script = line.getLBR_TaxBaseType().getScript();
		
		if (script == null)
			return TYPE_RATE_OR_IVA;
		
		Interpreter bsh = new Interpreter ();
		
		try
		{
			includedTaxes.clear();
			bsh.set("includedTaxes", includedTaxes);
			Object calcType = bsh.eval (script);
			//
			if (calcType != null && calcType instanceof Integer)
				return (Integer) calcType;
		}
		catch (EvalError e)
		{
			log.warning("Invalid script");
			return TYPE_RATE_OR_IVA;
		}
		
		return TYPE_RATE_OR_IVA;
	}	//	getCalculationType

	/**
	 * 	Set Description
	 */
	public void setDescription ()
	{
		String description = "";
		X_LBR_TaxLine[] lines = getLines();
		
		for (X_LBR_TaxLine line : lines)
		{
			MLBRTaxName tax = new MLBRTaxName (getCtx(), line.getLBR_TaxName_ID(), null);
			String name = tax.getName().trim();
			String rate = String.format("%,.2f", line.getLBR_TaxRate());
			description += ", " + name + "-" + rate;
		}

		if (description.startsWith(", ")) 
			description = description.substring(2);
		
		if (description.equals("")) 
			description = null;

		setDescription(description);
	}	//	setDescription

	/**
	 *  Copy the current MTax and return a new copy of the Object
	 *  
	 *  @return MTax newTax
	 */
	public MLBRTax copyTo ()
	{
		MLBRTax newTax = new MLBRTax(getCtx(), 0, get_TrxName());
		newTax.setDescription(getDescription());
		newTax.save(get_TrxName());
		copyLinesTo(newTax);
		//
		return newTax;
	}	//	copyTo

	/**
	 *  Copy lines from the current MTax to the newTax param
	 *  
	 * 	@param MLBRTax newTax
	 */
	public void copyLinesTo (MLBRTax newTax)
	{
		//	Delete old lines
		newTax.deleteLines();

		MLBRTaxLine[] lines = getLines();
		for (int i=0; i<lines.length; i++)
		{
			MLBRTaxLine newLine = new MLBRTaxLine (getCtx(), 0, get_TrxName());
			newLine.setLBR_Tax_ID(newTax.getLBR_Tax_ID());
			newLine.setLBR_TaxName_ID(lines[i].getLBR_TaxName_ID());
			newLine.setLBR_TaxRate(lines[i].getLBR_TaxRate());
			newLine.setLBR_TaxBase(lines[i].getLBR_TaxBase());
			newLine.setLBR_TaxBaseAmt(lines[i].getLBR_TaxBaseAmt());
			newLine.setLBR_TaxAmt(lines[i].getLBR_TaxAmt());
			newLine.setLBR_PostTax(lines[i].isLBR_PostTax());
			newLine.setIsTaxIncluded(lines[i].isTaxIncluded());
			newLine.setLBR_LegalMessage_ID(lines[i].getLBR_LegalMessage_ID());
			newLine.setLBR_TaxStatus_ID(lines[i].getLBR_TaxStatus_ID());
			newLine.setQty(lines[i].getQty());
			newLine.setLBR_TaxBaseType_ID(lines[i].getLBR_TaxBaseType_ID());
			newLine.setLBR_TaxListAmt(lines[i].getLBR_TaxListAmt());
			newLine.save(get_TrxName());
		}

		newTax.setDescription();
		newTax.save();
	} 	//	copyLinesTo

	/**
	 *  Copy lines from the current MTax to the newTax param
	 * 	
	 * 	@param LBR_Tax_ID
	 */
	public void copyLinesTo (int LBR_Tax_ID)
	{
		if (LBR_Tax_ID == 0)
			return;

		MLBRTax newTax = new MLBRTax(getCtx(), LBR_Tax_ID, get_TrxName());
		copyLinesTo (newTax);
	} 	//	copyLinesTo

	/**
	 *  	Get Lines
	 *  
	 *  @return MLBRTaxLine[] lines
	 */
	public MLBRTaxLine[] getLines ()
	{
		String whereClause = "LBR_Tax_ID = ?";

		MTable table = MTable.get(getCtx(), X_LBR_TaxLine.Table_Name);
		Query q =  new Query(getCtx(), table, whereClause, get_TrxName());
		q.setParameters(new Object[]{getLBR_Tax_ID()});

		List<MLBRTaxLine> list = q.list();
		MLBRTaxLine[] lines = new MLBRTaxLine[list.size()];
		return list.toArray(lines);
	} 	//	getLines

	/**
	 * 	Apaga as linhas
	 */
	public void deleteLines ()
	{
		String sql = "DELETE FROM LBR_TaxLine " +
        	         "WHERE LBR_Tax_ID=" + getLBR_Tax_ID();
		DB.executeUpdate(sql, get_TrxName());
	}	//	deleteLines

	/**
	 * 	Apaga as Linhas antes de apagar o registro
	 */
	public boolean delete (boolean force, String trxName)
	{
		deleteLines ();
		return super.delete (force, trxName);
	}	//	delete
	
	/**
	 * 		Retorna o registro do imposto baseado na pesquisa
	 * 
	 * 		N�o usar este m�todo em Callouts, pois a Callout pode acion�=lo antes que 
	 * 			a linha tenha sido salva.
	 * 
	 * 	@param Order Line
	 * 	@return Object Array (Taxes, Legal Msg, CFOP and CST) 
	 */
	public static Object[] getTaxes (MOrderLine ol)
	{
		MOrder o = new MOrder (Env.getCtx(), ol.getC_Order_ID(), null);
		MProduct p = new MProduct (Env.getCtx(), ol.getM_Product_ID(), null);
		MOrgInfo oi = MOrgInfo.get(Env.getCtx(), o.getAD_Org_ID(), null);
		
		MBPartner bp = new MBPartner (Env.getCtx(), o.getC_BPartner_ID(), null);
		MBPartnerLocation bpLoc = new MBPartnerLocation (Env.getCtx(), o.getBill_Location_ID(), null); 
		//
		return getTaxes (o.getC_DocTypeTarget_ID(), o.isSOTrx(), o.get_ValueAsString("LBR_TransactionType"), p, oi, bp, bpLoc, o.getDateAcct());
	}	//	getTaxes
	
	/**
	 * 		Retorna o registro do imposto baseado na pesquisa
	 * 
	 * @param Order
	 * @param Order Line
	 * @param Product
	 * @param Organization Info
	 * @param Business Partner
	 * @param Date Acct
	 * @return Object Array (Taxes, Legal Msg, CFOP and CST) 
	 */
	@SuppressWarnings("deprecation")
	public static Object[] getTaxes (int C_DocTypeTarget_ID, boolean isSOTrx, String lbr_TransactionType, MProduct p, 
			MOrgInfo oi, MBPartner bp, MBPartnerLocation bpLoc, Timestamp dateAcct)
	{
		I_W_C_BPartner bpW = POWrapper.create(bp, I_W_C_BPartner.class);
		I_W_M_Product pW = POWrapper.create(p, I_W_M_Product.class);
		Properties ctx = Env.getCtx();
		//
		Map<Integer, MLBRTaxLine> taxes = new HashMap<Integer, MLBRTaxLine>();
		//
		int LBR_LegalMessage_ID 	= 0;
		int LBR_CFOP_ID				= 0;
		String lbr_TaxStatus 		= "";
		//
		
		/**
		 * 	Organization
		 */
		log.info ("######## Processing Tax for Organization: " + oi + ", Taxes: " + new MLBRTax(ctx, oi.get_ValueAsInt("LBR_Tax_ID"), null));
		processTaxes(taxes, oi.get_ValueAsInt("LBR_Tax_ID"));
		
		/**
		 * 	NCM
		 *	FIXME: Criar o campo de NCM na OV
		 */
		if (p.getM_Product_ID() > 0 && pW.getLBR_NCM_ID() > 0)
		{
			MLBRNCM ncm = new MLBRNCM (Env.getCtx(), pW.getLBR_NCM_ID(), null);
			I_W_LBR_NCM ncmW = POWrapper.create(ncm, I_W_LBR_NCM.class);
			X_LBR_NCMTax ncmTax = ncm.getLBR_NCMTax(oi.getAD_Org_ID(), bpLoc.getC_Location().getC_Region_ID(), dateAcct);
			//
			if (ncmTax != null)
			{
				hasSubstitution = ncmTax.isLBR_HasSubstitution();
				log.info ("######## Processing Tax for NCM Line: " + ncmTax + ", Taxes: " + new MLBRTax(ctx, ncmTax.getLBR_Tax_ID(), null));
				processTaxes(taxes, ncmTax.getLBR_Tax_ID());
			}
			else
			{
				hasSubstitution = ncmW.isLBR_HasSubstitution();
				log.info ("######## Processing Tax for NCM: " + ncm + ", Taxes: " + new MLBRTax(ctx, ncmW.getLBR_Tax_ID(), null));
				processTaxes(taxes, ncmW.getLBR_Tax_ID());	//	Legacy
			}
		}
		
		/**
		 * 	Matriz de ICMS
		 */
		MLBRICMSMatrix mICMS = MLBRICMSMatrix.get (ctx, oi.getAD_Org_ID(), (oi.getC_Location_ID() < 1 ? -1 : oi.getC_Location().getC_Region_ID()), bpLoc.getC_Location().getC_Region_ID(), dateAcct, null);
		//
		if (mICMS != null && mICMS.getLBR_Tax_ID() > 0 && !MProduct.PRODUCTTYPE_Service.equals(p.getProductType()))
		{
			log.info ("######## Processing Tax for ICMS Matrix: " + mICMS + ", Taxes: " + new MLBRTax(ctx, mICMS.getLBR_Tax_ID(), null));
			processTaxes(taxes, mICMS.getLBR_Tax_ID());
			//
			if (hasSubstitution && mICMS.getLBR_STTax_ID() > 0)
			{
				log.info ("######## Processing Tax for ICMS ST Matrix: " + mICMS + ", Taxes: " + new MLBRTax(ctx, mICMS.getLBR_STTax_ID(), null));
				processTaxes(taxes, mICMS.getLBR_STTax_ID());
			}
		}
		
		/**
		 * 	Matriz de ISS
		 */
		MLBRISSMatrix mISS = MLBRISSMatrix.get (ctx, oi.getAD_Org_ID(), bpLoc.getC_Location().getC_Region_ID(), 
				(bpLoc != null ? bpLoc.getC_Location().getC_City_ID() : 0), p.getM_Product_ID(), dateAcct, null);
		//
		if (MProduct.PRODUCTTYPE_Service.equals(p.getProductType()) && mISS != null && mISS.getLBR_Tax_ID() > 0)
		{
			log.info ("######## Processing Tax for ISS Matrix: " + mISS + ", Taxes: " + new MLBRTax(ctx, mISS.getLBR_Tax_ID(), null));
			processTaxes(taxes, mISS.getLBR_Tax_ID());
		}
		
		/**
		 * 	Janela de Configura��o de Impostos
		 */
		MLBRTaxConfiguration tc = MLBRTaxConfiguration.get (ctx, oi.getAD_Org_ID(), p.getM_Product_ID(), 
				pW.getLBR_FiscalGroup_Product_ID(), isSOTrx, null);
		//
		if (tc != null)
		{
			/**
			 * 	Product Group
			 */
			if (MLBRTaxConfiguration.LBR_EXCEPTIONTYPE_FiscalGroup.equals(tc.getLBR_ExceptionType()))
			{
				X_LBR_TaxConfig_ProductGroup tcpg = tc.getTC_ProductGroup (oi.getAD_Org_ID(), dateAcct);
				
				if (tcpg != null)
				{
					log.info ("######## Processing Tax for Product Group: " + tcpg + ", Taxes: " + new MLBRTax(ctx, tcpg.getLBR_Tax_ID(), null));
					processTaxes(taxes, tcpg.getLBR_Tax_ID());
					//
					if (tcpg.getLBR_LegalMessage_ID() > 0)
						LBR_LegalMessage_ID =  tcpg.getLBR_LegalMessage_ID();
					//
					if (tcpg.getLBR_TaxStatus() != null && tcpg.getLBR_TaxStatus().length() > 0)
						lbr_TaxStatus = tcpg.getLBR_TaxStatus() ;
				}
			}

			/**
			 * 	Product
			 */
			else if (MLBRTaxConfiguration.LBR_EXCEPTIONTYPE_Product.equals(tc.getLBR_ExceptionType()))
			{
				X_LBR_TaxConfig_Product tcp = tc.getTC_Product (oi.getAD_Org_ID(), dateAcct);
				
				if (tcp != null)
				{
					log.info ("######## Processing Tax for Product: " + tcp + ", Taxes: " + new MLBRTax(ctx, tcp.getLBR_Tax_ID(), null));
					processTaxes(taxes, tcp.getLBR_Tax_ID());
					//
					if (tcp.getLBR_LegalMessage_ID() > 0)
						LBR_LegalMessage_ID =  tcp.getLBR_LegalMessage_ID();
					//
					if (tcp.getLBR_TaxStatus() != null && tcp.getLBR_TaxStatus().length() > 0)
						lbr_TaxStatus = tcp.getLBR_TaxStatus() ;
				}
			}
			
			/**
			 * 	Region
			 */
			X_LBR_TaxConfig_Region tcr = tc.getTC_Region (oi.getAD_Org_ID(), oi.getC_Location().getC_Region_ID(), (bpLoc != null ? bpLoc.getC_Location().getC_Region_ID() : 0), dateAcct);
			
			if (tcr != null)
			{
				log.info ("######## Processing Tax for Region: " + tcr + ", Taxes: " + new MLBRTax(ctx, tcr.getLBR_Tax_ID(), null));
				processTaxes(taxes, tcr.getLBR_Tax_ID());
				//
				if (tcr.getLBR_LegalMessage_ID() > 0)
					LBR_LegalMessage_ID =  tcr.getLBR_LegalMessage_ID();
				//
				if (tcr.getLBR_TaxStatus() != null && tcr.getLBR_TaxStatus().length() > 0)
					lbr_TaxStatus = tcr.getLBR_TaxStatus() ;
			}
				
			/**
			 * 	Business Partner Group
			 */
			X_LBR_TaxConfig_BPGroup tcbpg = tc.getTC_BPGroup (oi.getAD_Org_ID(), (isSOTrx ? bpW.getLBR_FiscalGroup_Customer_ID() : bpW.getLBR_FiscalGroup_Customer_ID()), dateAcct);
			
			if (tcbpg != null)
			{
				log.info ("######## Processing Tax for BPartner Group: " + tcbpg + ", Taxes: " + new MLBRTax(ctx, tcbpg.getLBR_Tax_ID(), null));
				processTaxes(taxes, tcbpg.getLBR_Tax_ID());
				//
				if (tcbpg.getLBR_LegalMessage_ID() > 0)
					LBR_LegalMessage_ID =  tcbpg.getLBR_LegalMessage_ID();
				//
				if (tcbpg.getLBR_TaxStatus() != null && tcbpg.getLBR_TaxStatus().length() > 0)
					lbr_TaxStatus = tcbpg.getLBR_TaxStatus() ;
			}

			/**
			 * 	Business Partner
			 */
			X_LBR_TaxConfig_BPartner tcbp = tc.getTC_BPartner (oi.getAD_Org_ID(), bp.getC_BPartner_ID(), dateAcct);
			
			if (tcbp != null)
			{
				log.info ("######## Processing Tax for BPartner: " + tcbp + ", Taxes: " + new MLBRTax(ctx, tcbp.getLBR_Tax_ID(), null));
				processTaxes (taxes, tcbp.getLBR_Tax_ID());
				//
				if (tcbp.getLBR_LegalMessage_ID() > 0)
					LBR_LegalMessage_ID =  tcbp.getLBR_LegalMessage_ID();
				//
				if (tcbp.getLBR_TaxStatus() != null && tcbp.getLBR_TaxStatus().length() > 0)
					lbr_TaxStatus = tcbp.getLBR_TaxStatus();
			}
		}
		
		/**
		 * 	CFOP
		 */
		String lbr_DestionationType = null;
		
		/**
		 * 	No caso de SUFRAMA, definir como Zona Franca - FIXME
		 */
		if (bpW.getLBR_Suframa() != null && bpW.getLBR_Suframa().length() > 0)
			lbr_DestionationType = X_LBR_CFOPLine.LBR_DESTIONATIONTYPE_ZonaFranca;
		
		/**
		 * 	Importa��o ou Exporta��o
		 */
		else if (bpLoc != null && (oi.getC_Location_ID() < 1 || bpLoc.getC_Location().getC_Country_ID() != oi.getC_Location().getC_Country_ID()))
			lbr_DestionationType = X_LBR_CFOPLine.LBR_DESTIONATIONTYPE_Estrangeiro;
		
		/**
		 * 	Dentro do Estado
		 */
		else if (bpLoc != null && bpLoc.getC_Location().getC_Region_ID() == oi.getC_Location().getC_Region_ID())
			lbr_DestionationType = X_LBR_CFOPLine.LBR_DESTIONATIONTYPE_EstadosIdenticos;
		
		/**
		 * 	Fora do Estado
		 */
		else 
			lbr_DestionationType = X_LBR_CFOPLine.LBR_DESTIONATIONTYPE_EstadosDiferentes;
		
		X_LBR_CFOPLine cFOPLine = MLBRCFOP.chooseCFOP (oi.getAD_Org_ID(), C_DocTypeTarget_ID, pW.getLBR_ProductCategory_ID(), 
				(isSOTrx ? bpW.getLBR_CustomerCategory_ID() : bpW.getLBR_VendorCategory_ID()), 
				lbr_TransactionType, lbr_DestionationType, hasSubstitution, p.isManufactured(), null);
		//
		if (cFOPLine != null)
		{
			log.info ("######## Processing Tax for CFOP Line: " + cFOPLine + ", Taxes: " + new MLBRTax(ctx, cFOPLine.getLBR_Tax_ID(), null));
			processTaxes (taxes, cFOPLine.getLBR_Tax_ID());
			//
			if (cFOPLine.getLBR_LegalMessage_ID() > 0)
				LBR_LegalMessage_ID =  cFOPLine.getLBR_LegalMessage_ID();
			//
			if (cFOPLine.getLBR_TaxStatus() != null && cFOPLine.getLBR_TaxStatus().length() > 0)
				lbr_TaxStatus = cFOPLine.getLBR_TaxStatus();
			//
			LBR_CFOP_ID = cFOPLine.getLBR_CFOP_ID();
		}
		
		//	Tax Definition
		MLBRTaxDefinition[] taxesDef = MLBRTaxDefinition.get (oi.getAD_Org_ID(), bp.getC_BPartner_ID(), C_DocTypeTarget_ID, 
				(oi.getC_Location_ID() < 1 ? -1 : oi.getC_Location().getC_Region_ID()), (bpLoc != null ? bpLoc.getC_Location().getC_Region_ID() : 0),
				(isSOTrx ? bpW.getLBR_CustomerCategory_ID() : bpW.getLBR_VendorCategory_ID()), 
				(isSOTrx ? bpW.getLBR_FiscalGroup_Customer_ID() : bpW.getLBR_FiscalGroup_Vendor_ID()), pW.getLBR_FiscalGroup_Product_ID(), 
				pW.getLBR_NCM_ID(),  pW.getLBR_ProductCategory_ID(), hasSubstitution, isSOTrx, lbr_TransactionType, dateAcct);
		//
		for (MLBRTaxDefinition td : taxesDef)
		{
			log.info ("######## Processing Tax for Tax Definition: " + td + ", Taxes: " + new MLBRTax(ctx, td.getLBR_Tax_ID(), null));
			processTaxes (taxes, td.getLBR_Tax_ID());
			//
			if (td.getLBR_LegalMessage_ID() > 0)
				LBR_LegalMessage_ID =  td.getLBR_LegalMessage_ID();
			//
			if (td.getLBR_TaxStatus() != null && td.getLBR_TaxStatus().length() > 0)
				lbr_TaxStatus = td.getLBR_TaxStatus();
			//

			if (td.getLBR_CFOP_ID() > 0)
				LBR_CFOP_ID = td.getLBR_CFOP_ID();
		}
		
		return new Object[]{taxes, LBR_LegalMessage_ID, LBR_CFOP_ID, lbr_TaxStatus};
	}	//	getTaxes

	/**
	 * 	Ajusta os impostos
	 * 	@param taxes
	 * 	@param tcpg
	 */
	private static void processTaxes (Map<Integer, MLBRTaxLine> taxes, int LBR_Tax_ID)
	{
		if (LBR_Tax_ID < 1 || taxes == null)
			return;
		//
		MLBRTax tax = new MLBRTax (Env.getCtx(), LBR_Tax_ID, null);
		//
		for (MLBRTaxLine tl : tax.getLines())
		{
			if (taxes.containsKey(tl.getLBR_TaxName_ID()))
				taxes.remove(tl.getLBR_TaxName_ID());
			//
			taxes.put (tl.getLBR_TaxName_ID(), tl.copy());
		}
	}	//	processTaxes
	
	/**
	 * 	To String
	 */
	public String toString ()
	{
		return "MLBRTax [ID=" + get_ID() + ", Taxes=" + (getDescription() == null ? "" : getDescription()) + "]";
	}	//	toString
}	//	MLBRTax
