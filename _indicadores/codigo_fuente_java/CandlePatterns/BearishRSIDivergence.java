/*
 * Patrón no nativo, propuesto para SQX_Library el 2026-07-11 (segunda ronda).
 * Espejo de BullishRSIDivergence.java — ver justificación completa ahí y en
 * user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * Divergencia bajista: el precio hace un máximo más alto pero el RSI hace un
 * máximo más bajo en el swing correspondiente — señal de agotamiento alcista.
 */
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Bearish RSI Divergence pattern", display="Bearish RSI Divergence(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Divergencia bajista: el precio hace un maximo mas alto pero el RSI hace un maximo mas bajo en el swing correspondiente. Señal de agotamiento alcista. Patrón no nativo agregado a SQX_Library.")
@OppositeBlock("BullishRSIDivergence")
public class BearishRSIDivergence extends ConditionBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="1")
	public int Shift;

	@Parameter(defaultValue="30", minValue=10, maxValue=100, step=1)
	public int LookbackPeriod;

	@Parameter(defaultValue="2", minValue=1, maxValue=5, step=1)
	public int SwingStrength;

	@Parameter(defaultValue="14", minValue=2, maxValue=50, step=1)
	public int RSIPeriod;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private double simpleRSI(int shiftBack) {
		double sumGain = 0, sumLoss = 0;
		for (int i = 0; i < RSIPeriod; i++) {
			double curr = Chart.Close(shiftBack + i);
			double prev = Chart.Close(shiftBack + i + 1);
			double change = curr - prev;
			if (change > 0) sumGain += change; else sumLoss += -change;
		}
		double avgGain = sumGain / RSIPeriod;
		double avgLoss = sumLoss / RSIPeriod;
		if (avgLoss < 0.0000000001) return 100;
		double rs = avgGain / avgLoss;
		return 100 - (100 / (1 + rs));
	}

	private boolean isSwingHigh(int barShift) {
		double centerHigh = Chart.High(barShift);
		for (int s = 1; s <= SwingStrength; s++) {
			if (Chart.High(barShift - s) > centerHigh) return false;
			if (Chart.High(barShift + s) > centerHigh) return false;
		}
		return true;
	}

	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		int firstSwingShift = -1, secondSwingShift = -1;

		for (int i = Shift + SwingStrength; i <= Shift + LookbackPeriod - SwingStrength; i++) {
			if (isSwingHigh(i)) {
				if (firstSwingShift == -1) {
					firstSwingShift = i;
				} else {
					secondSwingShift = i;
					break;
				}
			}
		}

		if (firstSwingShift == -1 || secondSwingShift == -1) return false;

		double recentHigh = Chart.High(firstSwingShift);
		double olderHigh = Chart.High(secondSwingShift);

		double recentRSI = simpleRSI(firstSwingShift);
		double olderRSI = simpleRSI(secondSwingShift);

		return (recentHigh > olderHigh) && (recentRSI < olderRSI);
	}
}
