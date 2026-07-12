/*
 * Patrón de vela no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Pin Bar bajista (espejo de BullishPinBar.java): mecha superior larga
 * (>= 2/3 del rango total de la vela) + cuerpo pequeño en el extremo inferior.
 * Fuente: https://dailypriceaction.com/blog/forex-pin-bar-trading-strategy/
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Bearish Pin Bar pattern", display="Bearish Pin Bar(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Pin Bar bajista: mecha superior larga (rechazo) con cuerpo pequeño en el extremo inferior. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("BullishPinBar")
public class BearishPinBar extends ConditionBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="1")
	public int Shift;

	@Parameter(defaultValue="0.66", minValue=0.4, maxValue=0.9, step=0.01)
	public double MinWickRatio;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		double H = Chart.High(Shift);
		double L = Chart.Low(Shift);
		double O = Chart.Open(Shift);
		double C = Chart.Close(Shift);

		double totalRange = H - L;
		if (totalRange < 0.0000000001) return false;

		double bodyHigh = Math.max(O, C);
		double bodyLow = Math.min(O, C);
		double upperWick = H - bodyHigh;
		double lowerWick = bodyLow - L;

		boolean longUpperWick = (upperWick / totalRange) >= MinWickRatio;
		boolean smallOppositeWick = lowerWick <= (totalRange * (1 - MinWickRatio) * 0.5);

		return longUpperWick && smallOppositeWick;
	}
}
