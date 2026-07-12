/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Roofing Filter (John F. Ehlers). Combina un filtro pasa-altos (highpass) de 2 polos
 * (elimina componentes de periodo largo, por defecto >48 barras) con el Super Smoother
 * Filter (elimina ruido de alta frecuencia) — deja pasar únicamente la componente
 * cíclica del precio, aislada tanto de la tendencia de largo plazo como del ruido.
 * No existe ningún indicador nativo que aísle específicamente la componente cíclica
 * de esta forma (ni ADX/CHOP/ER, que miden régimen tendencia-vs-rango, ni ningún
 * suavizado simple).
 * Fuente: https://www.linnsoft.com/topic/super-smoother-and-roofing-filter
 *
 * Escrito siguiendo el patrón real de los indicadores nativos, con estado propio
 * (privados) para ambas etapas del filtro recursivo.
 */
package SQ.Blocks.Indicators.EhlersRoofingFilter;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ROOF) Ehlers Roofing Filter", display="EhlersRoofingFilter(@Chart@#HighpassPeriod#,#SmoothPeriod#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Roofing Filter (Ehlers). Aisla la componente ciclica del precio: pasa-altos (elimina tendencia larga) + Super Smoother (elimina ruido). Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0)
@ParameterSet(set="HighpassPeriod=48,SmoothPeriod=10")
public class EhlersRoofingFilter extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="HighpassPeriod", minValue=10, maxValue=250, defaultValue="48", step=1)
	public int HighpassPeriod;

	@Parameter(category="Default", name="SmoothPeriod", minValue=4, maxValue=100, defaultValue="10", step=1)
	public int SmoothPeriod;

	@Output(name = "Roof", color = Colors.Red)
	public DataSeries Value;

	private double alpha1;
	private double hp1 = 0, hp2 = 0;

	private double ssC1, ssC2, ssC3;
	private double filt1 = 0, filt2 = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		double angle1 = Math.toRadians(0.707 * 360.0 / HighpassPeriod);
		alpha1 = (Math.cos(angle1) + Math.sin(angle1) - 1) / Math.cos(angle1);
		hp1 = 0; hp2 = 0;

		double a1 = Math.exp(-1.414 * Math.PI / SmoothPeriod);
		double b1 = 2 * a1 * Math.cos(1.414 * Math.PI / SmoothPeriod);
		ssC2 = b1;
		ssC3 = -a1 * a1;
		ssC1 = 1 - ssC2 - ssC3;
		filt1 = 0; filt2 = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close0 = Chart.Close.get(0);

		if (getCurrentBar() < 2) {
			Value.set(0, 0);
			hp1 = 0; hp2 = 0; filt1 = 0; filt2 = 0;
			return;
		}

		double close1 = Chart.Close.get(1);
		double close2 = Chart.Close.get(2);

		double hpCoef = (1 - alpha1 / 2.0) * (1 - alpha1 / 2.0);
		double hp = hpCoef * (close0 - 2 * close1 + close2) + 2 * (1 - alpha1) * hp1 - (1 - alpha1) * (1 - alpha1) * hp2;

		double filt = ssC1 * (hp + hp1) / 2.0 + ssC2 * filt1 + ssC3 * filt2;

		Value.set(0, filt);

		hp2 = hp1;
		hp1 = hp;
		filt2 = filt1;
		filt1 = filt;
	}
}
