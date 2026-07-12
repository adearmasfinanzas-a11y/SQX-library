/*
 * Indicador no nativo, propuesto y aprobado para SQX_Library el 2026-07-07.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Kaufman's Efficiency Ratio (Perry J. Kaufman). Mide la eficiencia direccional del precio:
 * 1 = movimiento perfectamente eficiente (tendencia limpia), 0 = ruido sin dirección neta.
 * Formula: ER = |Precio[0] - Precio[n]| / SUMA(|Precio[i] - Precio[i-1]|, i=0..n-1)
 *
 * Escrito siguiendo el patrón real de los indicadores nativos de esta instalación
 * (SQ/Blocks/Indicators/CCI/CCI.java).
 */
package SQ.Blocks.Indicators.EfficiencyRatio;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="(ER) Kaufman's Efficiency Ratio", display="EfficiencyRatio(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Kaufman's Efficiency Ratio. 1 = tendencia eficiente, 0 = ruido sin dirección neta. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0.5, min=0, max=1, step=0.01)
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=20")
public class EfficiencyRatio extends IndicatorBlock {

	@Parameter
	public DataSeries Input;

	@Parameter(defaultValue="10", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Output(name = "ER", color = Colors.Blue)
	public DataSeries Value;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		int window = Math.min(getCurrentBar(), Period);

		if (window < 1) {
			Value.set(0, 0);
			return;
		}

		double netChange = Math.abs(Input.get(0) - Input.get(window));

		double sumChanges = 0;
		for (int idx = 0; idx < window; idx++) {
			sumChanges += Math.abs(Input.get(idx) - Input.get(idx + 1));
		}

		if (sumChanges < 0.0000000001) {
			Value.set(0, 0);
		} else {
			Value.set(0, netChange / sumChanges);
		}
	}
}
