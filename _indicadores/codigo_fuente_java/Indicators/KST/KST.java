/*
 * Indicador no nativo, propuesto para SQX_Library el 2026-07-11.
 * Ver justificación y fuentes en: user/SQX_Library/_indicadores/indicadores_propuestos.json
 *
 * KST - Know Sure Thing (Martin Pring). Suma ponderada de 4 medias moviles de
 * Rate-of-Change en distintos periodos (10/15/20/30, suavizadas con SMA 10/10/10/15),
 * con pesos 1/2/3/4. Oscilador de momentum multi-horizonte en un solo valor.
 * Fuente: ChartSchool (StockCharts) - ficha propia del indicador
 *
 * Escrito siguiendo el patron real de los indicadores nativos (AverageCalculator SMA
 * encadenado sobre una serie de Rate-of-Change calculada a mano).
 */
package SQ.Blocks.Indicators.KST;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(KST) Know Sure Thing", display="KST(@Chart@)[#Shift#]", returnType = ReturnTypes.Number)
@Help("KST - Know Sure Thing (Pring). Suma ponderada de 4 ROC suavizados en distintos horizontes. Indicador no nativo agregado a SQX_Library.")
@Indicator(oscillator=true, middleValue=0, min=-50, max=50, step=0.1)
@ParameterSet(set="Roc1=10,Roc2=15,Roc3=20,Roc4=30,Sma1=10,Sma2=10,Sma3=10,Sma4=15,SignalPeriod=9")
public class KST extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(category="Default", name="Roc1", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int Roc1;
	@Parameter(category="Default", name="Roc2", minValue=2, maxValue=100, defaultValue="15", step=1)
	public int Roc2;
	@Parameter(category="Default", name="Roc3", minValue=2, maxValue=100, defaultValue="20", step=1)
	public int Roc3;
	@Parameter(category="Default", name="Roc4", minValue=2, maxValue=100, defaultValue="30", step=1)
	public int Roc4;

	@Parameter(category="Default", name="Sma1", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int Sma1;
	@Parameter(category="Default", name="Sma2", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int Sma2;
	@Parameter(category="Default", name="Sma3", minValue=2, maxValue=100, defaultValue="10", step=1)
	public int Sma3;
	@Parameter(category="Default", name="Sma4", minValue=2, maxValue=100, defaultValue="15", step=1)
	public int Sma4;

	@Parameter(category="Default", name="SignalPeriod", minValue=2, maxValue=100, defaultValue="9", step=1)
	public int SignalPeriod;

	@Output(name = "KST", color = Colors.Red)
	public DataSeries Value;

	@Output(name = "Signal", color = Colors.Blue)
	public DataSeries Signal;

	private AverageCalculator sma1Calc, sma2Calc, sma3Calc, sma4Calc, signalCalc;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		sma1Calc = new AverageCalculator(AverageCalculator.SMA, Sma1);
		sma2Calc = new AverageCalculator(AverageCalculator.SMA, Sma2);
		sma3Calc = new AverageCalculator(AverageCalculator.SMA, Sma3);
		sma4Calc = new AverageCalculator(AverageCalculator.SMA, Sma4);
		signalCalc = new AverageCalculator(AverageCalculator.SMA, SignalPeriod);
	}

	//------------------------------------------------------------------------

	private double rocAt(int period) {
		int back = Math.min(period, getCurrentBar());
		double past = Chart.Close.get(back);
		double now = Chart.Close.get(0);
		return (past != 0) ? 100.0 * (now - past) / past : 0;
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		sma1Calc.onBarUpdate(rocAt(Roc1), getCurrentBar());
		sma2Calc.onBarUpdate(rocAt(Roc2), getCurrentBar());
		sma3Calc.onBarUpdate(rocAt(Roc3), getCurrentBar());
		sma4Calc.onBarUpdate(rocAt(Roc4), getCurrentBar());

		double kst = sma1Calc.getValue() * 1 + sma2Calc.getValue() * 2 + sma3Calc.getValue() * 3 + sma4Calc.getValue() * 4;

		signalCalc.onBarUpdate(kst, getCurrentBar());

		Value.set(0, kst);
		Signal.set(0, signalCalc.getValue());
	}
}
