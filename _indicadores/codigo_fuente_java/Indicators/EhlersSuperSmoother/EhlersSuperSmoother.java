/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Super Smoother Filter (John F. Ehlers). Filtro de suavizado de 2 polos (IIR),
 * basado en filtros analógicos de aeroespacio — reduce ruido de alta frecuencia
 * con mucho menos retraso (lag) que una EMA/SMA convencional del mismo período de corte.
 * Fuente: https://www.mesasoftware.com/papers/EhlersFilters.pdf
 *
 * Escrito siguiendo el patrón real de los indicadores nativos, usando estado propio
 * (privados) en vez de un Calculator nativo, ya que este filtro no corresponde a
 * ninguno de los Calculators disponibles (AverageCalculator/HighestCalculator/etc.).
 */
package SQ.Blocks.Indicators.EhlersSuperSmoother;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(SSF) Ehlers Super Smoother Filter", display="EhlersSuperSmoother(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Super Smoother Filter (Ehlers). Suavizado de 2 polos con mucho menos retraso que SMA/EMA equivalente. Indicador no nativo agregado a SQX_Library.")
@ParameterSet(set="Period=10")
public class EhlersSuperSmoother extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Period", minValue=4, maxValue=100, defaultValue="10", step=1)
	public int Period;

	@Output(name = "SSF", color = Colors.Red)
	public DataSeries Value;

	private double filt1 = 0, filt2 = 0;
	private double c1, c2, c3;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		double a1 = Math.exp(-1.414 * Math.PI / Period);
		double b1 = 2 * a1 * Math.cos(1.414 * Math.PI / Period);
		c2 = b1;
		c3 = -a1 * a1;
		c1 = 1 - c2 - c3;
		filt1 = 0;
		filt2 = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close0 = Chart.Close.get(0);
		double close1 = (getCurrentBar() > 0) ? Chart.Close.get(1) : close0;

		double filt;
		if (getCurrentBar() < 2) {
			filt = close0;
		} else {
			filt = c1 * (close0 + close1) / 2.0 + c2 * filt1 + c3 * filt2;
		}

		Value.set(0, filt);

		filt2 = filt1;
		filt1 = filt;
	}
}
