package org.idempierelbr.nfe.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MCity;
import org.compiere.model.MCountry;
import org.compiere.model.MDocType;
import org.compiere.model.MLocation;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MProduct;
import org.compiere.model.MRegion;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTax;
import org.compiere.model.MUOM;
import org.compiere.model.X_C_City;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempierelbr.core.util.AdempiereLBR;
import org.idempierelbr.core.util.BPartnerUtil;
import org.idempierelbr.core.util.RemoverAcentos;
import org.idempierelbr.core.util.TextUtil;
import org.idempierelbr.nfe.beans.AdicoesDI;
import org.idempierelbr.nfe.beans.COFINSBean;
import org.idempierelbr.nfe.beans.COFINSSTBean;
import org.idempierelbr.nfe.beans.ChaveNFE;
import org.idempierelbr.nfe.beans.Cobranca;
import org.idempierelbr.nfe.beans.CobrancaGrupoDuplicata;
import org.idempierelbr.nfe.beans.CobrancaGrupoFatura;
import org.idempierelbr.nfe.beans.DadosNFE;
import org.idempierelbr.nfe.beans.DeclaracaoDI;
import org.idempierelbr.nfe.beans.DetalhesProdServBean;
import org.idempierelbr.nfe.beans.EnderDest;
import org.idempierelbr.nfe.beans.EnderEmit;
import org.idempierelbr.nfe.beans.ICMSBean;
import org.idempierelbr.nfe.beans.IdentDest;
import org.idempierelbr.nfe.beans.IdentEmit;
import org.idempierelbr.nfe.beans.IdentLocRetirada;
import org.idempierelbr.nfe.beans.IdentLocalEntrega;
import org.idempierelbr.nfe.beans.IdentNFE;
import org.idempierelbr.nfe.beans.IIBean;
import org.idempierelbr.nfe.beans.IPIBean;
import org.idempierelbr.nfe.beans.InfAdiFisco;
import org.idempierelbr.nfe.beans.InfECFRefBean;
import org.idempierelbr.nfe.beans.InfNFEProdutorRefBean;
import org.idempierelbr.nfe.beans.InfNFERefBean;
import org.idempierelbr.nfe.beans.NFERefBean;
import org.idempierelbr.nfe.beans.ObsContribGrupo;
import org.idempierelbr.nfe.beans.ObsFiscoGrupo;
import org.idempierelbr.nfe.beans.PISBean;
import org.idempierelbr.nfe.beans.PISSTBean;
import org.idempierelbr.nfe.beans.ProcessoRefGrupo;
import org.idempierelbr.nfe.beans.ProdutosNFEBean;
import org.idempierelbr.nfe.beans.Transporte;
import org.idempierelbr.nfe.beans.TransporteGrupo;
import org.idempierelbr.nfe.beans.TransporteGrupoVeiculos;
import org.idempierelbr.nfe.beans.TransporteLacres;
import org.idempierelbr.nfe.beans.TransporteReboque;
import org.idempierelbr.nfe.beans.TransporteRetencao;
import org.idempierelbr.nfe.beans.TransporteVol;
import org.idempierelbr.nfe.beans.TributosInciBean;
import org.idempierelbr.nfe.beans.Valores;
import org.idempierelbr.nfe.beans.ValoresICMS;
import org.idempierelbr.nfe.beans.ISSQNBean;
import org.idempierelbr.nfe.model.MLBRDocLineDetailsNfe;
import org.idempierelbr.nfe.model.MLBRNotaFiscal;
import org.idempierelbr.nfe.model.MLBRNotaFiscalDocRef;
import org.idempierelbr.nfe.model.MLBRNotaFiscalLine;
import org.idempierelbr.nfe.model.MLBRNotaFiscalPackage;
import org.idempierelbr.nfe.model.MLBRNotaFiscalPay;
import org.idempierelbr.nfe.model.MLBRNotaFiscalPaySched;
import org.idempierelbr.nfe.model.MLBRNotaFiscalTax;
import org.idempierelbr.nfe.model.MLBRNotaFiscalTransp;
import org.idempierelbr.nfe.model.X_LBR_NotaFiscalNote;
import org.idempierelbr.nfe.model.X_LBR_NotaFiscalProc;
import org.idempierelbr.nfe.model.X_LBR_NotaFiscalTrailer;
import org.idempierelbr.nfe.util.AssinaturaDigital;
import org.idempierelbr.nfe.util.BPartnerUtilNfe;
import org.idempierelbr.nfe.util.NFeUtil;
import org.idempierelbr.nfe.util.ValidaXML;
import org.idempierelbr.tax.model.MLBRCFOP;
import org.idempierelbr.tax.model.MLBRNCM;
import org.idempierelbr.tax.model.X_LBR_TaxGroup;

import br.gov.sp.fazenda.dsge.brazilutils.uf.UF;

import com.thoughtworks.xstream.XStream;

public class NFeXMLGenerator {
	/** Log				*/
	private static CLogger log = CLogger.getCLogger(NFeXMLGenerator.class);

	/** XML File Extension */
	public static final String FILE_EXT = "-nfe.xml";
	
	/**
	 * Gera o corpo da NF
	 * 
	 * @param LBR_NotaFiscal_ID
	 * @param trxName Transa��o
	 * @return
	 */
	public static String geraCorpoNFe (Properties ctx, int LBR_NotaFiscal_ID, String trxName) throws Exception {
		log.fine("Generating XML for NF-e");

		MLBRNotaFiscal nf = new MLBRNotaFiscal(ctx, LBR_NotaFiscal_ID, trxName);
		if (LBR_NotaFiscal_ID == 0)
			return "Nota Fiscal doesn't exist";

		XStream xstream = new XStream();
		xstream.autodetectAnnotations(true);

		// A. Dados da Nota Fiscal eletr�nica
		DadosNFE dados = new DadosNFE();
		xstream.alias("infNFe", DadosNFE.class);
		xstream.useAttributeFor(DadosNFE.class, "versao");
		dados.setVersao(NFeUtil.VERSAO);
		xstream.useAttributeFor(DadosNFE.class, "Id");
		nf.setVersionNo(NFeUtil.VERSAO);

		// DADOS DA ORG DE VENDA/COMPRA
		MOrg org = new MOrg(ctx, nf.getAD_Org_ID(), trxName);
		MOrgInfo orgInfo = MOrgInfo.get(ctx, org.get_ID(), trxName);
		int linked2OrgC_BPartner_ID = org.getLinkedC_BPartner_ID(trxName);
		
		if (linked2OrgC_BPartner_ID < 1)
			return "Nenhum Parceiro vinculado � Organiza��o";
		
		MBPartner bpLinked2Org = new MBPartner(ctx, linked2OrgC_BPartner_ID, trxName);

		MLocation orgLoc    = new MLocation(ctx, orgInfo.getC_Location_ID(), trxName);
		MRegion  orgRegion  = new MRegion(ctx, orgLoc.getC_Region_ID(), trxName);
		MCountry orgCountry = orgLoc.getCountry();
		X_C_City orgCity = BPartnerUtil.getX_C_City(ctx,orgLoc,trxName);

		// DADOS DO CLIENTE
		MBPartner bp = new MBPartner(ctx, nf.getC_BPartner_ID(), trxName);
		MBPartnerLocation bpartLoc = new MBPartnerLocation(ctx, nf.getC_BPartner_Location_ID(), trxName);
		MLocation bpLoc = bpartLoc.getLocation(false);
		if(bpLoc.getC_Country_ID() == 0){
			return "Erro Parceiro sem Pais Cadastrado";
		}

		X_C_City bpCity = BPartnerUtil.getX_C_City(ctx,bpLoc,trxName);

		// Dados do documento da NF
		String modNF = nf.getLBR_NFeModel();
		String serie = nf.getLBR_NFeSerie();

		/**
		 * Indicador da forma de pagamento:
		 * 0 � pagamento � vista
		 * 1 � pagamento � prazo
		 * 2 � outros
		 */
		String indPag = nf.getPaymentRule();

		//	Tipo de Documento
		MDocType docType = new MDocType(ctx, nf.getC_DocType_ID(), trxName);
		
		/** Identifica��o do Ambiente (1 - Produ��o; 2 - Homologa��o) */
		String tpAmb = docType.get_ValueAsString("LBR_NFeEnv");

		/** Formato de impress�o do DANFE (1 - Retrato; 2 - Paisagem) */
		String tpImp = docType.get_ValueAsString("LBR_DANFEFormat");

		/**
		 * Processo de emiss�o utilizado com a seguinte codifica��o:
		 * 0 - emiss�o de NF-e com aplicativo do contribuinte
		 * 1 - emiss�o de NF-e avulsa pelo Fisco
		 * 2 - emiss�o de NF-e avulsa, pelo contribuinte com seu certificado digital, atrav�s do site do Fisco
		 * 3 - emiss�o de NF-e pelo contribuinte com aplicativo fornecido pelo Fisco
		 */
		String procEmi = "0";
		
		/** Vers�o do aplicativo utilizado no processo de emiss�o */
		String verProc = NFeUtil.VERSAO_APP;

		/**
		 * Forma de emiss�o da NF-e:
		 * 1 - Normal
		 * 2 - Conting�ncia FS
		 * 3 - Conting�ncia SCAN
		 * 4 - Conting�ncia DPEC
		 * 5 - Conting�ncia FSDA
		 */
		String tpEmis = nf.getLBR_NFeTpEmis();

		/**
		 * Finalidade da emiss�o da NF-e:
		 * 1 - NFe normal
		 * 2 - NFe complementar
		 * 3 - NFe de ajuste
		 */
		String FinNFE = nf.getLBR_FinNFe();
		
		// Identifica��o NFE
		if (FinNFE.equals("2"))
		{
			// TODO: refatorar
			/*xstream.alias("NFref", NFERefenciadaBean.class);
			nfereferencia.setRefNFe(nf.getlbr_NFRefere().getlbr_NFeID());
			identNFe.setNFref(nfereferencia);*/
		}
		
		/**
		 * CRT
		 * 1 � Simples Nacional (SN)
		 * 2 � Simples Nacional � excesso de sublimite de receita bruta;
		 * 3 � Regime Normal (TN)
		 */
		String CRT = MSysConfig.getValue ("LBR_ICMS_REGIME", "TN", nf.getAD_Client_ID(), nf.getAD_Org_ID());
		
		if (CRT.equals("TN"))
			CRT = "3";
		else if (CRT.equals("SN"))
			CRT = "1";

		Timestamp datedoc = nf.getDateDoc();
		Timestamp dateSaiEnt = nf.getDateDelivered();
		String aamm = TextUtil.timeToString(datedoc, "yyMM");
		String orgCPNJ = TextUtil.toNumeric(bpLinked2Org.get_ValueAsString("LBR_CNPJ"));

		ChaveNFE chaveNFE = new ChaveNFE();
		chaveNFE.setAAMM(aamm);
		chaveNFE.setCUF(BPartnerUtil.getRegionCode(orgLoc));	//UF EMITENTE
		chaveNFE.setCNPJ(orgCPNJ);
		chaveNFE.setMod(modNF);
		chaveNFE.setSerie(TextUtil.lPad(serie, 3));
		chaveNFE.setTpEmis(tpEmis);
		chaveNFE.setNNF(TextUtil.lPad(nf.getDocumentNo(), 9));

		// Gera codigo fiscal para nota
		Random random = new Random();
		chaveNFE.setCNF(TextUtil.lPad("" + random.nextInt(99999999), 8));

		int digitochave = chaveNFE.gerarDigito();
		dados.setId("NFe" + chaveNFE.toString() + digitochave);

		String DEmi 	= TextUtil.timeToString(datedoc, "yyyy-MM-dd");
		String TEmi 	= TextUtil.timeToString(datedoc, "HH:mm:ss");
		String DSaiEnt 	= TextUtil.timeToString(dateSaiEnt, "yyyy-MM-dd");
		String TSaiEnt 	= TextUtil.timeToString(dateSaiEnt, "HH:mm:ss");
		String timezone = AdempiereLBR.getTimezone(nf.getAD_Client_ID(), nf.getAD_Org_ID());
		
		// B. Identifica��o da Nota Fiscal eletr�nica
		IdentNFE identNFe = new IdentNFE();
		identNFe.setcUF(chaveNFE.getCUF());
		identNFe.setcNF(chaveNFE.getCNF());
		identNFe.setNatOp(RemoverAcentos.remover(TextUtil.checkSize(nf.getLBR_NFeNatOp(), 1, 60)));
		identNFe.setIndPag(indPag);
		identNFe.setMod(chaveNFE.getMod());
		identNFe.setSerie(serie);
		identNFe.setnNF(nf.getDocumentNo());
		identNFe.setdhEmi(DEmi + "T" + TEmi + timezone);
		if (nf.getDateDelivered() != null)
		{
			identNFe.setdhSaiEnt(DSaiEnt + "T" + TSaiEnt + timezone);
		}
		identNFe.setTpNF(nf.getLBR_NFE_OperationType());
		identNFe.setIdDest(nf.getLBR_NFE_DestinationType());
		
		Integer orgCityCode = 0;
		if (orgCity != null  && orgCity.get_Value("lbr_CityCode") != null)
			orgCityCode = Integer.parseInt(orgCity.get_ValueAsString("LBR_CityCode"));

		Integer ocorrenciaCityCode = 0;
		X_C_City ocorrenciaCity = new X_C_City(ctx, nf.getC_City_ID(), trxName);		
		if (ocorrenciaCity.get_Value("LBR_CityCode") != null)
			ocorrenciaCityCode = Integer.parseInt(ocorrenciaCity.get_ValueAsString("LBR_CityCode"));

		identNFe.setcMunFG(ocorrenciaCityCode.toString());
		identNFe.setTpImp(tpImp);
		identNFe.setTpEmis(tpEmis);
		identNFe.setcDV("" + digitochave);
		identNFe.setTpAmb(tpAmb);
		identNFe.setFinNFe(FinNFE);
		identNFe.setIndFinal(nf.getLBR_NFeIndFinal());
		identNFe.setIndPres(nf.getLBR_NFeIndPres());
		identNFe.setProcEmi(procEmi);
		identNFe.setVerProc(verProc);
		
		// BA. Documento Fiscal Referenciado
		MLBRNotaFiscalDocRef[] docRefs = nf.getDocRefs();
		
		if (docRefs.length > 0) {
			String prefixException = "@Tab@ @LBR_DocRef@, @Field@ @IsMandatory@: ";
		
			for (MLBRNotaFiscalDocRef docRef : docRefs) {
				xstream.addImplicitCollection(IdentNFE.class, "NFrefs");
				xstream.alias("NFref", NFERefBean.class);
				
				NFERefBean nfRef = new NFERefBean();
				
				// NF-e
				if (docRef.getLBR_NFeDocRefType().equals("0")) {
					if (docRef.getLBR_NFeID() == null)
						return prefixException + "'@LBR_NFeID@'";
					
					if (!TextUtil.isNumber(docRef.getLBR_NFeID()) && docRef.getLBR_NFeID().trim().length() != 44)
						return "@Tab@ @LBR_DocRef@, @Field@ @invalid@: @LBR_NFeID@";
					
					nfRef.setRefNFe(docRef.getLBR_NFeID().trim());
				}
				// Modelo 1/1A
				else if (docRef.getLBR_NFeDocRefType().equals("2")) {
					InfNFERefBean refNF = new InfNFERefBean();
					
					MRegion region = new MRegion(ctx, docRef.getC_Region_ID(), trxName);
					String regionCode = region.get_ValueAsString("LBR_RegionCode");
					
					if (regionCode == null || regionCode.equals(""))
						return prefixException + "'@C_Region_ID@'";					
					refNF.setcUF(regionCode);
					
					if (docRef.getLBR_NFYearMonth() == null || docRef.getLBR_NFYearMonth().length() != 5)
						return "@Tab@ @LBR_DocRef@, @Field@ @invalid@: @LBR_NFYearMonth@";					
					refNF.setAAMM(docRef.getLBR_NFYearMonth().substring(3) + docRef.getLBR_NFYearMonth().substring(0,2));
					
					if (docRef.getLBR_CNPJ() == null || docRef.getLBR_CNPJ().equals(""))
						return prefixException + "'@LBR_CNPJ@'";					
					refNF.setCNPJ(docRef.getLBR_CNPJ());
					
					if (docRef.getLBR_NFModel() == null || docRef.getLBR_NFModel().equals(""))
						return prefixException + "'@LBR_NFModel@'";					
					refNF.setMod(docRef.getLBR_NFModel());
					
					if (docRef.getLBR_NFeSerie() == null || docRef.getLBR_NFeSerie().equals(""))
						return prefixException + "'@LBR_NFeSerie@'";					
					refNF.setSerie(docRef.getLBR_NFeSerie());
					
					if (docRef.getLBR_Document() == null || docRef.getLBR_Document().equals(""))
						return prefixException + "'@LBR_Document@'";					
					refNF.setnNF(docRef.getLBR_Document());

					nfRef.setRefNF(refNF);
				}
				// Produtor Rural
				else if (docRef.getLBR_NFeDocRefType().equals("3")) {
					InfNFEProdutorRefBean refNFP = new InfNFEProdutorRefBean();
					
					MRegion region = new MRegion(ctx, docRef.getC_Region_ID(), trxName);
					String regionCode = region.get_ValueAsString("LBR_RegionCode");
					
					if (regionCode == null || regionCode.equals(""))
						return prefixException + "'@C_Region_ID@'";					
					refNFP.setcUF(regionCode);
					
					if (docRef.getLBR_NFYearMonth() == null || docRef.getLBR_NFYearMonth().length() != 5)
						return "@Tab@ @LBR_DocRef@, @Field@ @invalid@: @LBR_NFYearMonth@";					
					refNFP.setAAMM(docRef.getLBR_NFYearMonth().substring(3) + docRef.getLBR_NFYearMonth().substring(0,2));
					
					if (docRef.getLBR_BPTypeBR() == null || docRef.getLBR_BPTypeBR().trim().equals(""))
						return prefixException + "'@LBR_BPTypeBR@'";
					
					if (docRef.getLBR_BPTypeBR().equals("PJ")) {
						if (docRef.getLBR_CNPJ() == null || docRef.getLBR_CNPJ().trim().equals(""))
							return prefixException + "'@LBR_CNPJ@'";
						refNFP.setCNPJ(docRef.getLBR_CNPJ());
					} else if (docRef.getLBR_BPTypeBR().equals("PF")) {
						if (docRef.getLBR_CPF() == null || docRef.getLBR_CPF().trim().equals(""))
							return prefixException + "'@LBR_CPF@'";
						refNFP.setCPF(docRef.getLBR_CPF());
					}
					
					if (docRef.isLBR_IsIEExempt())
						refNFP.setIE("ISENTO");
					else {
						if (docRef.getLBR_IE() == null || docRef.getLBR_IE().trim().equals(""))
							return prefixException + "'@LBR_IE@'";
						refNFP.setIE(docRef.getLBR_IE());
					}
					
					if (docRef.getLBR_NFProdModel() == null || docRef.getLBR_NFProdModel().equals(""))
						return prefixException + "'@LBR_NFProdModel@'";					
					refNFP.setMod(docRef.getLBR_NFProdModel());
					
					if (docRef.getLBR_NFeSerie() == null || docRef.getLBR_NFeSerie().equals(""))
						return prefixException + "'@LBR_NFeSerie@'";					
					refNFP.setSerie(docRef.getLBR_NFeSerie());
					
					if (docRef.getLBR_Document() == null || docRef.getLBR_Document().equals(""))
						return prefixException + "'@LBR_Document@'";					
					refNFP.setnNF(docRef.getLBR_Document());

					nfRef.setRefNFP(refNFP);
				}
				// CT-e
				else if (docRef.getLBR_NFeDocRefType().equals("1")) {
					if (docRef.getLBR_NFeID() == null)
						return prefixException + "'@LBR_NFeID@'";
					
					if (!TextUtil.isNumber(docRef.getLBR_NFeID()) && docRef.getLBR_NFeID().trim().length() != 44)
						return "@Tab@ @LBR_DocRef@, @Field@ @invalid@: @LBR_NFeID@";
					
					nfRef.setRefCTe(docRef.getLBR_NFeID().trim());
				}
				// Cupom Fiscal
				else if (docRef.getLBR_NFeDocRefType().equals("4")) {
					InfECFRefBean refECF = new InfECFRefBean();
					
					if (docRef.getLBR_ECFModel() == null || docRef.getLBR_ECFModel().trim().equals(""))
						return prefixException + "'@LBR_ECFModel@'";
					refECF.setMod(docRef.getLBR_ECFModel());
					
					if (docRef.getLBR_ECFNo() == null || docRef.getLBR_ECFNo().trim().equals(""))
						return prefixException + "'@LBR_ECFNo@'";
					refECF.setnECF(docRef.getLBR_ECFNo());
					
					if (docRef.getLBR_COONo() == null || docRef.getLBR_COONo().trim().equals(""))
						return prefixException + "'@LBR_COONo@'";
					refECF.setnCOO(docRef.getLBR_COONo());					
						
					nfRef.setRefECF(refECF);
				}
				
				identNFe.addNFref(nfRef);
			}
		}

		dados.setIde(identNFe);
		
		// C. Identifica��o do Emitente da Nota Fiscal eletr�nica
		IdentEmit emitente = new IdentEmit();
		emitente.setCNPJ(chaveNFE.getCNPJ());
		String orgNome = RemoverAcentos.remover(bpLinked2Org.getName());
		emitente.setxNome(orgNome);
		String orgXFant = RemoverAcentos.remover(bpLinked2Org.getName2());
		
		if (orgXFant == null || orgXFant.equals(""))
			orgXFant = orgNome;
		
		emitente.setxFant(orgXFant);
		
		EnderEmit enderEmit = new EnderEmit();
		enderEmit.setxLgr(RemoverAcentos.remover(orgLoc.getAddress1()));
		enderEmit.setNro(orgLoc.getAddress2());
		enderEmit.setxBairro(RemoverAcentos.remover(orgLoc.getAddress3()));
		
		if (orgLoc.getAddress4() != null)
			enderEmit.setxCpl(RemoverAcentos.remover(orgLoc.getAddress4()));
		
		enderEmit.setcMun(orgCityCode.toString());
		enderEmit.setxMun(RemoverAcentos.remover(orgCity.getName()));
		enderEmit.setUF(orgRegion.getName());
		enderEmit.setCEP(TextUtil.toNumeric(orgLoc.getPostal()));
		enderEmit.setcPais((Integer.parseInt(orgCountry.get_ValueAsString("LBR_CountryCode"))) + "");
		enderEmit.setxPais(AdempiereLBR.getCountry_trl(orgCountry));
		emitente.setEnderEmit(enderEmit);
		
		String orgIE = bpLinked2Org.get_ValueAsString("LBR_IE");
		UF uf = UF.valueOf(orgRegion.getName());
		// Valida��o IE
		orgIE = BPartnerUtilNfe.validaIE(orgIE,uf);
		
		if (orgIE == null)
			return "@LBR_Issuer@: @LBR_WrongIE@";

		emitente.setIE(orgIE);
		String orgIM = TextUtil.formatStringCodes(bpLinked2Org.get_ValueAsString("LBR_CCM"));
		
		if (orgIM == null || orgIM.equals(""))
			orgIM = "EM ANDAMENTO";
		
		emitente.setIM(orgIM);
		String orgCNAE = TextUtil.formatStringCodes(orgInfo.get_ValueAsString("LBR_CNAE"));
		emitente.setCNAE(orgCNAE);
		emitente.setCRT(CRT);
		dados.setEmit(emitente);

		// E. Identifica��o do Destinat�rio da Nota Fiscal eletr�nica
		IdentDest destinatario = new IdentDest();
		String bpCNPJ = TextUtil.formatStringCodes(bp.get_ValueAsString("LBR_CNPJ"));
		
		if (bpCNPJ.length() == 11)
			destinatario.setCPF(bpCNPJ);
		else
			destinatario.setCNPJ(bpCNPJ);
		
		destinatario.setxNome(RemoverAcentos.remover(bp.getName()));
		
		EnderDest enderDest = new EnderDest();
		enderDest.setXLgr(RemoverAcentos.remover(bpLoc.getAddress1()));
		enderDest.setNro(RemoverAcentos.remover(bpLoc.getAddress2()));
		
		if (bpLoc.getAddress3() != null)
			enderDest.setXBairro(RemoverAcentos.remover(bpLoc.getAddress3()));
		else
			return "@LBR_Recipient@: @LBR_WrongAddress3@";

		Integer bpCityCode = 0;
		
		if (bpCity == null && bpLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
			return "@LBR_Recipient@: @LBR_CantFindCity@";

		if (bpCity != null && bpCity.get_Value("LBR_CityCode") != null) {
			bpCityCode = Integer.parseInt(bpCity.get_ValueAsString("LBR_CityCode"));
			
			if ((bpCityCode == null || bpCityCode.intValue() == 0) &
					bpLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
				return "@LBR_Recipient@: @LBR_CantFindIBGECityCode@";

			enderDest.setXMun(RemoverAcentos.remover(bpLoc.getCity()));
		} else {
			bpCityCode = Integer.valueOf(BPartnerUtil.EXTCOD);
			enderDest.setXMun(BPartnerUtil.EXTMUN);
		}
		
		enderDest.setCMun(bpCityCode.toString());
		
		if (bpCityCode.intValue() == Integer.valueOf(BPartnerUtil.EXTCOD)) {
			enderDest.setUF(BPartnerUtil.EXTREG);
		} else {
			enderDest.setUF(bpLoc.getRegionName());

			if (bpLoc.getPostal() != null)
				enderDest.setCEP(TextUtil.checkSize(TextUtil.formatStringCodes(bpLoc.getPostal()), 8, 8, '0'));
			else
				return "@LBR_Recipient@: @LBR_WrongPostalCode@";
		}

		MCountry bpCountry = new MCountry(ctx, bpLoc.getC_Country_ID(), trxName);
		enderDest.setCPais((Integer.parseInt(bpCountry.get_ValueAsString("LBR_CountryCode"))) + "");
		enderDest.setXPais(AdempiereLBR.getCountry_trl(bpCountry));

		String bpIE = bp.get_ValueAsString("LBR_IE");
		boolean bpIEIsento = bp.get_ValueAsBoolean("LBR_IsIEExempt");
		uf = UF.valueOf(bpLoc.getRegionName());

		// Valida��o IE
		bpIE = BPartnerUtilNfe.validaIE(bpIE,uf);
		
		if (bpIE == null && !bpIEIsento) {
			return "@LBR_Recipient@: @LBR_WrongIE@";
		}
		
		if (!bpIEIsento)
			destinatario.setIndIEDest("1");
		else if (bpIEIsento)
			destinatario.setIndIEDest("2");

		destinatario.setIE(bpIE);							

		String suframa = TextUtil.formatStringCodes(bp.get_ValueAsString("LBR_Suframa"));
		
		if (!suframa.isEmpty())	
			destinatario.setISUF(suframa);
		
		// Homologa��o
		if (tpAmb.equals("2")){
			if (uf != null){
				destinatario.setCPF(null);
				destinatario.setCNPJ("99999999000191");
			}
			destinatario.setxNome("NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL");
			destinatario.setIE("");
		}
		
		destinatario.setEnderDest(enderDest);
		dados.setDest(destinatario);
		
		// F. Identifica��o do Local de Retirada
		if (nf.getLBR_BP_Pickup_ID() > 0) {
			IdentLocRetirada retirada = new IdentLocRetirada();
			MBPartner bpRetirada = new MBPartner(ctx, nf.getLBR_BP_Pickup_ID(), trxName);
			String bpRetiradaCNPJ = TextUtil.formatStringCodes(bpRetirada.get_ValueAsString("LBR_CNPJ"));
			
			if (bpRetiradaCNPJ.length() == 11)
				retirada.setCPF(bpCNPJ);
			else
				retirada.setCNPJ(bpCNPJ);
			
			MBPartnerLocation bpartRetiradaLoc = new MBPartnerLocation(ctx, nf.getLBR_BP_PickupLocation_ID(), trxName);
			MLocation bpRetiradaLoc = bpartRetiradaLoc.getLocation(false);
			
			if (bpRetiradaLoc.getC_Country_ID() == 0)
				return "@LBR_PickupLocation@: @LBR_WrongCountry@";
			
			retirada.setxLgr(RemoverAcentos.remover(bpRetiradaLoc.getAddress1()));
			retirada.setNro(RemoverAcentos.remover(bpRetiradaLoc.getAddress2()));
			
			if (bpRetiradaLoc.getAddress3() != null)
				retirada.setxBairro(RemoverAcentos.remover(bpRetiradaLoc.getAddress3()));
			else
				return "@LBR_PickupLocation@: @LBR_WrongAddress3@";
			
			if (bpRetiradaLoc.getAddress4() != null)
				retirada.setxCpl(RemoverAcentos.remover(bpRetiradaLoc.getAddress4()));

			Integer bpRetiradaCityCode = 0;
			X_C_City bpRetiradaCity = BPartnerUtil.getX_C_City(ctx, bpRetiradaLoc, trxName);
			
			if (bpRetiradaCity == null && bpRetiradaLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
				return "@LBR_PickupLocation@: @LBR_CantFindCity@";

			if (bpRetiradaCity != null && bpRetiradaCity.get_Value("LBR_CityCode") != null) {
				bpRetiradaCityCode = Integer.parseInt(bpRetiradaCity.get_ValueAsString("LBR_CityCode"));
				
				if ((bpRetiradaCityCode == null || bpRetiradaCityCode.intValue() == 0) &
						bpRetiradaLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
					return "@LBR_PickupLocation@: @LBR_CantFindIBGECityCode@";
				
				retirada.setxMun(RemoverAcentos.remover(bpRetiradaLoc.getCity()));
			} else {
				bpRetiradaCityCode = Integer.valueOf(BPartnerUtil.EXTCOD);
				retirada.setxMun(BPartnerUtil.EXTMUN);
			}
			
			retirada.setcMun(bpRetiradaCityCode.toString());
			
			if (bpRetiradaCityCode.intValue() == Integer.valueOf(BPartnerUtil.EXTCOD))
				retirada.setUF(BPartnerUtil.EXTREG);
			else
				retirada.setUF(bpRetiradaLoc.getRegionName());
			
			dados.setRetirada(retirada);
		}
		
		// Identificacao do Local de Entrega
		if (nf.getLBR_BP_Ship_ID() > 0) {
			IdentLocalEntrega entrega = new IdentLocalEntrega();
			MBPartner bpEntrega = new MBPartner(ctx, nf.getLBR_BP_Ship_ID(), trxName);
			
			String bpEntregaCNPJ = TextUtil.formatStringCodes(bpEntrega.get_ValueAsString("LBR_CNPJ"));
			
			if (bpEntregaCNPJ.length() == 11)
				entrega.setCPF(bpCNPJ);
			else
				entrega.setCNPJ(bpCNPJ);
			
			MBPartnerLocation bpartEntregaLoc = new MBPartnerLocation(ctx, nf.getLBR_BP_ShipLocation_ID(), trxName);
			MLocation bpEntregaLoc = bpartEntregaLoc.getLocation(false);
			
			if(bpEntregaLoc.getC_Country_ID() == 0)
				return "@LBR_ShipLocation@: @LBR_WrongCountry@";
			
			entrega.setxLgr(RemoverAcentos.remover(bpEntregaLoc.getAddress1()));
			entrega.setNro(RemoverAcentos.remover(bpEntregaLoc.getAddress2()));
			
			if (bpEntregaLoc.getAddress3() != null)
				entrega.setxBairro(RemoverAcentos.remover(bpEntregaLoc.getAddress3()));
			else
				return "@LBR_ShipLocation@: @LBR_WrongAddress3@";
			
			if (bpEntregaLoc.getAddress4() != null)
				entrega.setxCpl(RemoverAcentos.remover(bpEntregaLoc.getAddress4()));

			Integer bpEntregaCityCode = 0;
			X_C_City bpEntregaCity = BPartnerUtil.getX_C_City(ctx, bpEntregaLoc, trxName);
			
			if (bpEntregaCity == null && bpEntregaLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
				return "@LBR_ShipLocation@: @LBR_CantFindCity@";

			if (bpEntregaCity != null && bpEntregaCity.get_Value("LBR_CityCode") != null) {
				bpEntregaCityCode = Integer.parseInt(bpEntregaCity.get_ValueAsString("LBR_CityCode"));
				
				if ((bpEntregaCityCode == null || bpEntregaCityCode.intValue() == 0) &
						bpEntregaLoc.getC_Country_ID() == BPartnerUtil.BRASIL)
					return "@LBR_ShipLocation@: @LBR_CantFindIBGECityCode@";
				
				entrega.setxMun(RemoverAcentos.remover(bpEntregaLoc.getCity()));
			} else {
				bpEntregaCityCode = Integer.valueOf(BPartnerUtil.EXTCOD);
				entrega.setxMun(BPartnerUtil.EXTMUN);
			}
			
			entrega.setcMun(bpEntregaCityCode.toString());
			
			if (bpEntregaCityCode.intValue() == Integer.valueOf(BPartnerUtil.EXTCOD))
				entrega.setUF(BPartnerUtil.EXTREG);
			else
				entrega.setUF(bpEntregaLoc.getRegionName());
			
			dados.setEntrega(entrega);
		}
		
		// H. Detalhamento de Produtos e Servi�os da NF-e
		MLBRNotaFiscalLine[] nfLines = nf.getLines();
		int linhaNF = 1;
		
		for (MLBRNotaFiscalLine nfLine : nfLines) {
			String prefixLineMandatory = ", @Field@ @IsMandatory@: ";
			
			MLBRDocLineDetailsNfe details = MLBRDocLineDetailsNfe.getOfPO(nfLine);
			ProdutosNFEBean produtos = new ProdutosNFEBean();
			
			//Informacoes informacoes = new Informacoes();
			//ImpostoDIBean impostodi = new ImpostoDIBean(); // DI

			MProduct prdt = new MProduct(ctx, nfLine.getM_Product_ID(), null);
			produtos.setcProd(RemoverAcentos.remover(details.getProductValue()));
			
			if (prdt.getUPC() == null || (prdt.getUPC().length() < 12 || prdt.getUPC().length() > 14))
				produtos.setcEAN("");
			else
				produtos.setcEAN(prdt.getUPC());
			
			produtos.setxProd(RemoverAcentos.remover(details.getProductName()));
			
			MLBRNCM ncm = new MLBRNCM(ctx, details.getLBR_NCM_ID(), trxName);
			String ncmValue = ncm.getValue();

			if (ncmValue == null)
				return "@Line@: " + nfLine.getLine() + ": @LBR_MissingNCM@";

			produtos.setNCM(TextUtil.toNumeric(ncmValue));
			
			MLBRCFOP cfop = new MLBRCFOP(ctx, details.getLBR_CFOP_ID(), trxName);
			String cfopName = cfop.getValue();
			
			if (cfopName == null || cfopName.equals(""))
				return "@Line@: " + nfLine.getLine() + ": @LBR_InvalidCFOP@";

			if (!cfop.validateCFOP(nf.getLBR_NFE_OperationType().equals("1") ? true : false, orgLoc, bpLoc, false))
				return "@Line@: " + nfLine.getLine() + ": @LBR_InvalidCFOP@";

			produtos.setCFOP(TextUtil.formatStringCodes(cfopName));
			
			if (nfLine.getC_UOM_ID() < 1)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@C_UOM_ID@'";
			
			MUOM uomC = new MUOM(ctx, nfLine.getC_UOM_ID(), trxName);
			produtos.setuCom(RemoverAcentos.remover(uomC.getUOMSymbol()));
			
			if (nfLine.getQty() == null)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@Qty@'";
			
			produtos.setqCom(TextUtil.bigdecimalToString(nfLine.getQty(),4));
			
			if (nfLine.getPriceActual() == null)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@PriceActual@'";
			
			produtos.setvUnCom(TextUtil.bigdecimalToString(nfLine.getPriceActual(),10));
			
			if (details.getLBR_GrossAmt() == null)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@LBR_GrossAmt@'";
			
			produtos.setvProd(TextUtil.bigdecimalToString(details.getLBR_GrossAmt()));
			
			if (details.getLBR_UPCTax() == null || (details.getLBR_UPCTax().length() < 12 || details.getLBR_UPCTax().length() > 14))
				produtos.setcEANTrib("");
			else
				produtos.setcEANTrib(details.getLBR_UPCTax());
			
			if (details.getLBR_UOMTax_ID() < 1)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@LBR_UOMTax_ID@'";
			
			MUOM uomT = new MUOM(ctx, details.getLBR_UOMTax_ID(), trxName);			
			produtos.setuTrib(RemoverAcentos.remover(uomT.getUOMSymbol()));
			
			if (details.getLBR_QtyTax() == null)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@LBR_QtyTax@'";
			
			produtos.setqTrib(TextUtil.bigdecimalToString(details.getLBR_QtyTax(), 4));
			
			if (details.getLBR_PriceTax() == null)
				return "@Line@: " + nfLine.getLine() + prefixLineMandatory + "'@LBR_PriceTax@'";
			
			produtos.setvUnTrib(TextUtil.bigdecimalToString(details.getLBR_PriceTax(), 10));
			
			// TODO: Refatorar valores do frete e do seguro
			/*if (nf.getFreightAmt().signum() == 1 && nfLine.getFreightAmt(nf.getTotalLines(), nf.getFreightAmt()).compareTo(Env.ZERO) != 0) //FRETE
				produtos.setvFrete(TextUtil.bigdecimalToString(nfLine.getFreightAmt(nf.getTotalLines(), nf.getFreightAmt())));
				
			if (nf.getlbr_InsuranceAmt().signum() == 1) //SEGURO
				produtos.setvSeg(TextUtil.bigdecimalToString(nfLine.getInsuranceAmt(nf.getTotalLines(), nf.getlbr_InsuranceAmt())));*/
			
			if (details.getDiscountAmt() != null && details.getDiscountAmt().compareTo(Env.ZERO) != 0)
				produtos.setvDesc(TextUtil.bigdecimalToString(details.getDiscountAmt().abs(),2));
			
			if (details.getSurcharges() != null && details.getSurcharges().compareTo(Env.ZERO) != 0)
				produtos.setvOutro(TextUtil.bigdecimalToString(details.getSurcharges()));
			
			produtos.setIndTot(details.isLBR_IsGrossAmtInTotal() ? "1" : "0");
			
			// Declara��o de Importa��o
			// TODO: Refatorar DI
			/*DeclaracaoDI declaracao = new DeclaracaoDI();
			//Importa��o - nDI Obrigat�rio
			if (nfLine.getlbr_CFOPName() != null &&
					nfLine.getlbr_CFOPName().startsWith("3")){
				if (nfLine.get_Value("LBR_NFDI_ID") == null)
					return "Linha: " + nfLine.getLine() + " CFOP Importa��o. " +
							"DI Obrigat�rio!";
			}

			//	DI e Adi��es
			if (nfLine.get_Value("LBR_NFDI_ID") != null) {
				X_LBR_NFDI di = new X_LBR_NFDI(Env.getCtx(), (Integer) nfLine.get_Value("LBR_NFDI_ID"), null);
				//
				declaracao.setcExportador(RemoverAcentos.remover(di.getlbr_CodExportador()));
				declaracao.setdDesemb(TextUtil.timeToString(di.getlbr_DataDesemb(), "yyyy-MM-dd"));
				declaracao.setdDI(TextUtil.timeToString(di.getDateTrx(), "yyyy-MM-dd"));
				declaracao.setnDI(di.getlbr_DI());
				declaracao.setUFDesemb(di.getlbr_BPRegion());
				declaracao.setxLocDesemb(RemoverAcentos.remover(di.getlbr_LocDesemb()));

				AdicoesDI adicao = new AdicoesDI();
				adicao.setcFabricante(RemoverAcentos.remover(nfLine.get_ValueAsString("Manufacturer")));
				adicao.setnAdicao(nfLine.get_ValueAsString("lbr_NumAdicao"));
				adicao.setnSeqAdic(nfLine.get_ValueAsString("lbr_NumSeqItem"));
			  //adicao.setVDescDI(Env.ZERO);	//TODO
				adicao.setnDI(di.getlbr_DI());
				declaracao.addAdi(adicao);
				produtos.setDI(declaracao);
			} //DI
			*/			
			
			// Pedido de Compra
			if (details.getPOReference() != null && !details.getPOReference().trim().isEmpty())
				produtos.setxPed(details.getPOReference());
			
			if (details.getLBR_POReferenceLine() > 0)
				produtos.setnItemPed(String.valueOf(details.getLBR_POReferenceLine()));

			// Grupo Diversos
			if (details.getLBR_FCIControlNo() != null)
				produtos.setnFCI(details.getLBR_FCIControlNo());
			
			// Tributos incidentes no Produto ou Servi�o
			TributosInciBean impostos = new TributosInciBean();
			String desc = RemoverAcentos.remover(TextUtil.removeEOL(details.get_ValueAsString("Memo")));
			
			if (desc != null && !desc.equals(""))
				dados.add(new DetalhesProdServBean(produtos, impostos, linhaNF++, desc));
			else
				dados.add(new DetalhesProdServBean(produtos, impostos, linhaNF++));

			xstream.alias("det", DetalhesProdServBean.class);
			xstream.useAttributeFor(DetalhesProdServBean.class, "nItem");
			xstream.addImplicitCollection(DadosNFE.class, "det");

			// ICMS
			ICMSBean icms = null;
			try {
				icms = nfLine.getICMSBean();
			} catch(AdempiereException e) {
				return e.getMessage();
			}
			if (icms != null)
				impostos.setICMS(icms);
			
			// IPI
			IPIBean ipi = null;
			try {
				ipi = nfLine.getIPIBean();
			} catch(AdempiereException e) {
				return e.getMessage();
			}
			if (ipi != null)
				impostos.setIPI(ipi);
			
			// Import Tax
			IIBean ii = null;
			try {
				ii = nfLine.getImportTaxBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (ii != null)
				impostos.setII(ii);
			
			// PIS
			PISBean pis = null;
			try {
				pis = nfLine.getPISBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (pis != null)
				impostos.setPIS(pis);
			
			// PIS ST
			PISSTBean pisST = null;
			try {
				pisST = nfLine.getPISSTBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (pisST != null)
				impostos.setPISST(pisST);
			
			// COFINS
			COFINSBean cofins = null;
			try {
				cofins = nfLine.getCOFINSBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (cofins != null)
				impostos.setCOFINS(cofins);
			
			// COFINS ST
			COFINSSTBean cofinsST = null;
			try {
				cofinsST = nfLine.getCOFINSSTBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (cofinsST != null)
				impostos.setCOFINSST(cofinsST);
			
			// ISSQN
			ISSQNBean issqn = null;
			try {
				issqn = nfLine.getISSQNBean();
			} catch (AdempiereException e) {
				return e.getMessage();
			}
			if (issqn != null)
				impostos.setISSQN(issqn);
		}
		
		// Total da NF-e
		ValoresICMS valoresicms = new ValoresICMS();
		valoresicms.setvBC(TextUtil.ZERO_STRING); // vBC - BC do ICMS
		valoresicms.setvICMS(TextUtil.ZERO_STRING); // vICMS - Valor Total do ICMS
		// TODO: Refatorar icms desonerado
		valoresicms.setvICMSDeson(TextUtil.ZERO_STRING); // vICMS - Valor Total do ICMS desonerado
		valoresicms.setvBCST(TextUtil.ZERO_STRING); // vBCST - BC do ICMS ST
		valoresicms.setvST(TextUtil.ZERO_STRING); // vST - Valor Total do ICMS ST
		valoresicms.setvProd(TextUtil.bigdecimalToString(nf.getTotalLines())); // vProd - Valor Total dos produtos e servi�os
		// TODO: refatorar valores de frete/seguro/despesas acess�rias
		valoresicms.setvFrete(TextUtil.ZERO_STRING); // vFrete - Valor Total do Frete
		valoresicms.setvSeg(TextUtil.ZERO_STRING); // vSeg - Valor Total do Seguro
		valoresicms.setvOutro(TextUtil.ZERO_STRING); // vOutro - Despesa acess�rias
		BigDecimal vDesc = nf.getDiscount(); // Valor do Desconto total da NF
		valoresicms.setvDesc(TextUtil.bigdecimalToString(vDesc)); // vDesc - Valor Total do Desconto
		valoresicms.setvII(TextUtil.ZERO_STRING); // vII - Valor Total do II
		valoresicms.setvIPI(TextUtil.ZERO_STRING); // vIPI - Valor Total do IPI
		valoresicms.setvPIS(TextUtil.ZERO_STRING); // vPIS - Valor do PIS
		valoresicms.setvCOFINS(TextUtil.ZERO_STRING); // vCOFINS - Valor do COFINS
		valoresicms.setvNF(TextUtil.bigdecimalToString(nf.getGrandTotal())); // vNF - Valor Total da NF-e
		
		MLBRNotaFiscalTax[] nfTaxes = nf.getTaxes(true);
		
		for (MLBRNotaFiscalTax nfTax : nfTaxes){
			MTax tax = new MTax(ctx, nfTax.getC_Tax_ID(), trxName);
			X_LBR_TaxGroup taxGroup = new X_LBR_TaxGroup(ctx, tax.get_ValueAsInt("LBR_TaxGroup_ID"), null);
			
			if (taxGroup.getName().toUpperCase().equals("ICMSST")) {
				valoresicms.setvBCST(TextUtil.bigdecimalToString(nfTax.getTaxBaseAmt())); // vBCST - BC do ICMS ST
				valoresicms.setvST(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vST - Valor Total do ICMS ST
			}

			if (taxGroup.getName().toUpperCase().equals("ICMS")) {
				valoresicms.setvBC(TextUtil.bigdecimalToString(nfTax.getTaxBaseAmt())); // vBC - BC do ICMS
				valoresicms.setvICMS(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vICMS - Valor Total do ICMS
			}

			if (taxGroup.getName().toUpperCase().equals("PIS")) {
				valoresicms.setvPIS(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vPIS - Valor do PIS
			}

			if (taxGroup.getName().toUpperCase().equals("COFINS")) {
				valoresicms.setvCOFINS(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vCOFINS - Valor do COFINS
			}

			if (taxGroup.getName().toUpperCase().equals("IPI")) {
				valoresicms.setvIPI(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vIPI - Valor Total do IPI
			}

			if (taxGroup.getName().toUpperCase().equals("II")) {
				valoresicms.setvII(TextUtil.bigdecimalToString(nfTax.getTaxAmt())); // vII - Valor Total do II
			}
		}

		Valores valores = new Valores();
		valores.setICMSTot(valoresicms);
		dados.setTotal(valores);
	
		// X. Informa��es de Transporte da NF-e
		MLBRNotaFiscalTransp transp = nf.getTransp();
		Transporte transporte = new Transporte();
		TransporteGrupo transgrupo = new TransporteGrupo();
		
		if (transp == null)
			return "@Tab@ @LBR_Transportation@: @NoLines@";
		
		transporte.setModFrete(transp.getLBR_NFeModShipping());

		if (transp.getM_Shipper_ID() > 0) {
			MBPartner bpTransp = new MBPartner(ctx, transp.getM_Shipper().getC_BPartner_ID(), trxName);
			
			if (bpTransp.get_ID() < 1) {
				log.warning("Nota Fiscal " + nf.getDocumentNo()  + ": shipper doesn't have a linked Business Partner");
				transgrupo.setxNome(RemoverAcentos.remover(transp.getM_Shipper().getName()));
			} else {
				if (bpTransp.get_ValueAsString("LBR_BPTypeBR").equals("PJ"))
					transgrupo.setCNPJ(TextUtil.toNumeric(bpTransp.get_ValueAsString("LBR_CNPJ")));
				else if (bpTransp.get_ValueAsString("LBR_BPTypeBR").equals("PF"))
					transgrupo.setCPF(TextUtil.toNumeric(bpTransp.get_ValueAsString("LBR_CPF")));
				
				transgrupo.setxNome(RemoverAcentos.remover(bpTransp.getName()));

				String shipperIE = bpTransp.get_ValueAsString("LBR_IE");	
				boolean shipperIEIsento = bpTransp.get_ValueAsBoolean("LBR_IsIEExempt");
				MBPartnerLocation bpTranspLoc = new MBPartnerLocation(ctx, transp.getLBR_M_Shipper_Location_ID(), trxName);
				MLocation transpLoc = bpTranspLoc.getLocation(false);
				UF shipperUF = UF.valueOf(transpLoc.getRegionName());

				if (shipperUF != null) {
					if (!transp.isLBR_IsICMSTaxExempt()) {
						if (shipperIEIsento)
							transgrupo.setIE("ISENTO");
						else {
							// Valida��o IE
							shipperIE = BPartnerUtilNfe.validaIE(shipperIE, shipperUF);
							
							if (shipperIE == null)
								return "@Tab@ @LBR_Transportation@: @LBR_WrongIE@";
							
							transgrupo.setIE(shipperIE);
						}
					}

					String end = (transpLoc.getAddress1() == null ? "" : transpLoc.getAddress1() + ", ") + 	//	Rua
						(transpLoc.getAddress2() == null ? "" : transpLoc.getAddress2() + " - ") + 			//	N�mero
						(transpLoc.getAddress4() == null ? "" : transpLoc.getAddress4() + " - ") +			//	Complemento
						(transpLoc.getAddress3() == null ? "" : transpLoc.getAddress3());					//	Bairro

					transgrupo.setxEnder(RemoverAcentos.remover(end));
					transgrupo.setxMun(RemoverAcentos.remover(transpLoc.getCity()));
					transgrupo.setUF(RemoverAcentos.remover(transpLoc.getRegionName()));
				}
			}
			
			transporte.setTransporta(transgrupo);
		}
		
		String prefixTranspMandatory = "@Tab@ @LBR_Transportation@, @Field@ @IsMandatory@: ";
		
		if (transp.getLBR_TaxAmt() != null) {
			TransporteRetencao transRet = new TransporteRetencao();
			
			if (transp.getChargeAmt() == null)
				return prefixTranspMandatory + "'@ChargeAmt@'";
			transRet.setvServ(TextUtil.bigdecimalToString(transp.getChargeAmt()));
			if (transp.getLBR_TaxBaseAmt() == null)
				return prefixTranspMandatory + "'@LBR_TaxBaseAmt@'";
			transRet.setvBCRet(TextUtil.bigdecimalToString(transp.getLBR_TaxBaseAmt()));
			if (transp.getLBR_TaxRate() == null)
				return prefixTranspMandatory + "'@LBR_TaxRate@'";
			transRet.setpICMSRet(TextUtil.bigdecimalToString(transp.getLBR_TaxRate()));
			if (transp.getLBR_TaxAmt() == null)
				return prefixTranspMandatory + "'@LBR_TaxAmt@'";
			transRet.setvICMSRet(TextUtil.bigdecimalToString(transp.getLBR_TaxAmt()));
			
			MLBRCFOP cfop = new MLBRCFOP(ctx, transp.getLBR_CFOP_ID(), trxName);
			String cfopName = cfop.getValue();
			
			if (cfopName == null || cfopName.equals(""))
				return prefixTranspMandatory + "'@LBR_CFOP_ID@'";
			
			transRet.setCFOP(TextUtil.formatStringCodes(cfopName));
			
			if (transp.getC_City_ID() < 1)
				return prefixTranspMandatory + "'@C_City_ID@'";
				
			MCity city = new MCity(ctx, transp.getC_City_ID(), trxName);
			
			if (city != null  && city.get_Value("lbr_CityCode") != null)
				transRet.setcMunFG(city.get_ValueAsString("LBR_CityCode"));
	
			transporte.setRetTransp(transRet);
		}
		
		if (transp.getLBR_NFeTranspVehicleType() != null && !transp.getLBR_NFeTranspVehicleType().equals("")) {
			xstream.addImplicitCollection(Transporte.class, "reboques");
			xstream.alias("reboque", TransporteReboque.class);
			
			// Ve�culo / Reboque
			if (transp.getLBR_NFeTranspVehicleType().equals("0")) {
				TransporteGrupoVeiculos transVeic = new TransporteGrupoVeiculos(); 
				
				if (transp.getLBR_LicensePlate() == null)
					return prefixTranspMandatory + "'@LBR_LicensePlate@'";
				transVeic.setPlaca(transp.getLBR_LicensePlate());
				
				MRegion region = new MRegion(ctx, transp.getLBR_LicensePlateRegion_ID(), trxName);
				String regionName = region.getName();
				
				if (regionName == null || regionName.equals(""))
					return prefixTranspMandatory + "'@LBR_LicensePlateRegion_ID@'";
				
				transVeic.setUF(regionName);
				transVeic.setRNTC(transp.getLBR_RNTC());
				transporte.setVeicTransp(transVeic);
				
				// Reboques
				X_LBR_NotaFiscalTrailer[] trailers = transp.getTrailers();
				
				for (X_LBR_NotaFiscalTrailer trailer : trailers) {
					String prefixRebMandatory = "@Tab@ @LBR_Trailer@, @Field@ @IsMandatory@: ";
					
					TransporteReboque transReb = new TransporteReboque();

					if (trailer.getLBR_LicensePlate() == null)
						return prefixRebMandatory + "'@LBR_LicensePlate@'";
					transReb.setPlaca(trailer.getLBR_LicensePlate());
					
					region = new MRegion(ctx, trailer.getLBR_LicensePlateRegion_ID(), trxName);
					regionName = region.getName();
					
					if (regionName == null || regionName.equals(""))
						return prefixRebMandatory + "'@LBR_LicensePlateRegion_ID@'";
					
					transReb.setUF(regionName);
					transReb.setRNTC(trailer.getLBR_RNTC());
					transporte.addReboque(transReb);
				}
			}
			// Balsa
			else if (transp.getLBR_NFeTranspVehicleType().equals("1")) {
				transporte.setBalsa(transp.getLBR_Ferry());
			}
			// Vag�o
			else if (transp.getLBR_NFeTranspVehicleType().equals("2")) {
				transporte.setVagao(transp.getLBR_Wagon());
			}
		}
		
		// Volumes
		MLBRNotaFiscalPackage[] packages = transp.getPackages();
		
		xstream.addImplicitCollection(Transporte.class, "vols");
		xstream.alias("vol", TransporteVol.class);
		
		for (MLBRNotaFiscalPackage pack : packages) {
			TransporteVol transVol = new TransporteVol();
			
			if (pack.getQty() != null)
				transVol.setqVol(TextUtil.bigdecimalToString(pack.getQty(), 0));
			
			if (pack.getC_UOM_ID() > 0) {
				MUOM uom = new MUOM(ctx, pack.getC_UOM_ID(), trxName);
				transVol.setEsp(uom.getUOMSymbol());
			}
			
			transVol.setMarca(pack.getLBR_BrandName());
			transVol.setnVol(pack.getLBR_Numeration());
			
			if (pack.getLBR_NetWeight() != null)
				transVol.setPesoL(TextUtil.bigdecimalToString(pack.getLBR_NetWeight(), 3));
			
			if (pack.getLBR_GrossWeight() != null)
				transVol.setPesoB(TextUtil.bigdecimalToString(pack.getLBR_GrossWeight(), 3));
			
			// Lacres
			if (pack.getLBR_SealNo() != null && !pack.getLBR_SealNo().trim().equals("")) {
				String[] lacresNo = pack.getLBR_SealNo().toString().split(";");
				for (String lacreNo : lacresNo) {
					if (!lacreNo.trim().equals("")) {
						xstream.addImplicitCollection(TransporteVol.class, "lacres");
						xstream.alias("lacres", TransporteLacres.class);
						
						TransporteLacres transLacre = new TransporteLacres();
						transLacre.setNLacre(lacreNo.trim());
						transVol.addLacre(transLacre);
					}
				}
			}
			
			transporte.addVol(transVol);
		}
		
		dados.setTransp(transporte);

		// Y. Dados da Cobran�a
		MLBRNotaFiscalPay[] pays = nf.getPay();
		
		if (pays.length > 0) {
			xstream.addImplicitCollection(Cobranca.class, "dups");
			xstream.alias("dup", CobrancaGrupoDuplicata.class);
			
			MLBRNotaFiscalPay pay = pays[0];
					
			if (pay != null && (pay.getLBR_Document() != null ||
					pay.getGrandTotal() != null ||
					pay.getDiscountAmt() != null ||
					pay.getNetAmtToInvoice() != null)) {
				
				Cobranca cobr = new Cobranca();
				CobrancaGrupoFatura cobrfat = null;
				CobrancaGrupoDuplicata cobrdup = null;
				
				cobrfat = new CobrancaGrupoFatura();
				cobrfat.setnFat(pay.getLBR_Document());
				
				if (pay.getGrandTotal() != null)
					cobrfat.setvOrig(TextUtil.bigdecimalToString(pay.getGrandTotal()));
				
				if (pay.getDiscountAmt() != null)
					cobrfat.setvDesc(TextUtil.bigdecimalToString(pay.getDiscountAmt()));
				
				if (pay.getNetAmtToInvoice() != null)
					cobrfat.setvLiq(TextUtil.bigdecimalToString(pay.getNetAmtToInvoice()));
			    
			    cobr.setFat(cobrfat);
		
			    //	Adiciona as duplicatas da fatura
			    MLBRNotaFiscalPaySched payScheds[] = pay.getPaySchedules();
			    
			    for (MLBRNotaFiscalPaySched paySched : payScheds) {
			    	cobrdup = new CobrancaGrupoDuplicata();
			    	cobrdup.setnDup(paySched.getLBR_Document());
					cobrdup.setdVenc(TextUtil.timeToString(paySched.getDueDate(), "yyyy-MM-dd"));
					
					if (paySched.getDueAmt() == null)
						return "@Tab@ @LBR_PaySchedule@, @Field@ @IsMandatory@: @DueAmt@";
					
					cobrdup.setvDup(TextUtil.bigdecimalToString(paySched.getDueAmt()));
					cobr.addDup(cobrdup);
				}
	
			    dados.setCobr(cobr);
			}
		}
		
		// TODO: Quando preparar DI, corrigir localiza��o/fun��o destas linhas
		xstream.alias("adi", AdicoesDI.class);
		xstream.addImplicitCollection(DeclaracaoDI.class, "adi");
		xstream.omitField(AdicoesDI.class, "nDI");
		
		// Z. Informa��es Adicionais da NF-e
		String fiscalInfo = nf.getLBR_FiscalInfo();
		String taxPayerInfo = nf.getLBR_TaxPayerInfo();
		X_LBR_NotaFiscalNote[] notes = nf.getNotes();
		X_LBR_NotaFiscalProc[] procs = nf.getProcs();
		
		if ((fiscalInfo != null && !fiscalInfo.trim().equals("")) ||
			(taxPayerInfo != null && !taxPayerInfo.trim().equals("")) ||
			notes.length > 0 ||
			procs.length > 0) {
			
			InfAdiFisco infAdi = new InfAdiFisco();
		
			if (fiscalInfo != null && !fiscalInfo.trim().equals(""))
				infAdi.setInfAdFisco(fiscalInfo);
			
			if (taxPayerInfo != null && !taxPayerInfo.trim().equals(""))
				infAdi.setInfCpl(taxPayerInfo);
			
			if (notes.length > 0) {
				for (X_LBR_NotaFiscalNote note : notes) {
					if (note.getLBR_NFeNoteType().equals("1")) {
						xstream.addImplicitCollection(InfAdiFisco.class, "obsConts");
						xstream.alias("obsCont", ObsContribGrupo.class);
						
						ObsContribGrupo obsContGrupo = new ObsContribGrupo();
						
						if (note.getColumnName() == null && !note.getColumnName().equals(""))
							return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @ColumnName@";
						
						obsContGrupo.setxCampo(note.getColumnName());
						
						if (note.getNote() == null && !note.getNote().equals(""))
							return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @LBR_Note@";
						
						obsContGrupo.setxTexto(note.getNote());
						infAdi.addObsCont(obsContGrupo);
					} else if (note.getLBR_NFeNoteType().equals("0")) {
						xstream.addImplicitCollection(InfAdiFisco.class, "obsFiscos");
						xstream.alias("obsFisco", ObsFiscoGrupo.class);
						
						ObsFiscoGrupo obsFiscoGrupo = new ObsFiscoGrupo();
						
						if (note.getColumnName() == null && !note.getColumnName().equals(""))
							return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @ColumnName@";
						
						obsFiscoGrupo.setxCampo(note.getColumnName());
						
						if (note.getNote() == null && !note.getNote().equals(""))
							return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @LBR_Note@";
						
						obsFiscoGrupo.setxTexto(note.getNote());
						infAdi.addObsFisco(obsFiscoGrupo);
					}
				}
			}
			
			if (procs.length > 0) {
				for (X_LBR_NotaFiscalProc proc : procs) {
					xstream.addImplicitCollection(InfAdiFisco.class, "procRefs");
					xstream.alias("procRef", ProcessoRefGrupo.class);
					
					ProcessoRefGrupo procRefGrupo = new ProcessoRefGrupo();
					
					if (proc.getProcessName() == null && !proc.getProcessName().equals(""))
						return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @ProcessName@";
					
					procRefGrupo.setnProc(proc.getProcessName());
					
					if (proc.getLBR_NFeProcOrigin() == null && !proc.getLBR_NFeProcOrigin().equals(""))
						return "@Tab@ @LBR_Note@, @Field@ @IsMandatory@: @LBR_NFeProcOrigin@";
					
					procRefGrupo.setIndProc(proc.getLBR_NFeProcOrigin());
					infAdi.addProcRef(procRefGrupo);
				}
			}			
			
			dados.setInfAdic(infAdi);
		}

		String nfeID = dados.getId().substring(3);
		String arquivoXML = nfeID + FILE_EXT;
		String NFeEmXML = NFeUtil.geraCabecNFe() + TextUtil.removeEOL(xstream.toXML(dados)) + NFeUtil.geraRodapNFe();
		
		try
		{
			log.fine("Signing NF-e XML");
			arquivoXML = TextUtil.generateTmpFile(NFeUtil.removeIndent(NFeEmXML), arquivoXML);
			AssinaturaDigital.Assinar(arquivoXML, orgInfo, AssinaturaDigital.RECEPCAO_NFE);
		}
		catch (Exception e){
			log.severe(e.getMessage());
			System.out.println(e.getMessage());
		}

		String retValidacao = "";

		File file = new File(arquivoXML);

		try{
			log.fine("Validating NF-e XML");

			retValidacao = NFeUtil.validateSize(file);
			if (retValidacao != null)
				return retValidacao;

			FileInputStream stream = new FileInputStream(file);
			InputStreamReader streamReader = new InputStreamReader(stream);
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(streamReader);

			String validar = "";
			String line    = null;
			while( (line=reader.readLine() ) != null ) {
				validar += line;
			}
			//
			retValidacao = ValidaXML.validaXML(validar);
		}
		catch (Exception e){
			log.severe(e.getMessage());
		}

		if (!retValidacao.equals("")){
			log.log(Level.SEVERE, retValidacao);
			return retValidacao;
		} else {
			//	Grava ID
			nf.setLBR_NFeID(nfeID);
			nf.save(trxName);
			//	Anexa o XML na NF
			MAttachment attachNFe = nf.createAttachment();
			attachNFe.addEntry(file);
			attachNFe.save(trxName);
		}

		return null;
	}
}
