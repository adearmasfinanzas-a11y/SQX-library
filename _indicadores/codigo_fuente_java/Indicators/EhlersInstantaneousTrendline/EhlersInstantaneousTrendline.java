/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Instantaneous Trendline (John F. Ehlers, "Cybernetic Analysis for Stocks and
 * Futures"). Estima la componente de tendencia instantánea del precio separándola
 * del componente cíclico, con un filtro recursivo de bajo retraso.
 * it = (a-(a*a/4))*src + 0.5*a*a*src[1] - (a-0.75*a*a)*src[2] + 2*(1-a)*it[1] - (1-a)*(1-a)*it[2]
 * Fuente: https://c.mql5.com/forextsd/forum/59/023inst.pdf (paper original de Ehlers)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos, con estado propio
 * (privados) ya que es un filtro recursivo no cubierto por los Calculators nativos.
 */
package SQ.Blocks.Indicators.EhlersInstantaneousTrendline;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ITREND) Ehlers Instantaneous Trendline", display="EhlersInstantaneousTrendline(@Chart@#Alpha#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("Instantaneous Trendline (Ehlers). Componente de tendencia instantanea del precio, separada del componente ciclico. Indicador no nativo agregado a SQX_Library.")
@ParameterSet(set="Alpha=0.07")
public class EhlersInstantaneousTrendline extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Alpha", minValue=0.01, maxValue=0.5, defaultValue="0.07", step=0.01)
	public double Alpha;

	@Output(name = "ITrend", color = Colors.Red)
	public DataSeries Value;

	private double it1 = 0, it2 = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		it1 = 0;
		it2 = 0;
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double src0 = Chart.Close.get(0);

		double it;
		if (getCurrentBar() < 2) {
			double h = Chart.High.get(0);
			double l = Chart.Low.get(0);
			it = (src0 + h + l) / 3.0;
		} else {
			double src1 = Chart.Close.get(1);
			double src2 = Chart.Close.get(2);
			double a = Alpha;

			it = (a - (a * a / 4.0)) * src0
				+ 0.5 * a * a * src1
				- (a - 0.75 * a * a) * src2
				+ 2 * (1 - a) * it1
				- (1 - a) * (1 - a) * it2;
		}

		Value.set(0, it);

		it2 = it1;
		it1 = it;
	}
}
