package span.problem;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class SpanProblemTests {

   public static void main(String[] args) {
      SpanProblemTests spanProblemTests = new SpanProblemTests();
      spanProblemTests.runSuite();
   }

   private void runSuite() {
      //doesn't really test much
      this.testGenerateRandomDatasetWithRandomScoresAndCompareParsedResult();
      //as all scores are the same, this verifies that all teams have the same rank and that this is invariant provided score is the same
      this.testGenerateRandomDatasetWithAllEqualScoresAndCompareParsedResult();

      //TODO the basic rig for testing is there, however, for brevity, we'll do them later
   }

   private void testGenerateRandomDatasetWithRandomScoresAndCompareParsedResult() {
      String tmpl = "Generating and comparing a set of <%d> random rows with random scores";
      this.testValidityOfGeneratedInput(tmpl, TestUtils.randomAndRandom, numGames -> {}, (thisScoreboard, parsedScoreboard) -> {
      });
   }

   private void testGenerateRandomDatasetWithAllEqualScoresAndCompareParsedResult() {
      String tmpl = "Generating and comparing a set of <%d> random rows with all same scores";
      AtomicInteger numGamesAtomicInteger = new AtomicInteger(0);
      this.testValidityOfGeneratedInput(tmpl,
         TestUtils.newRoundRobinAllZeroes(0),
         numGames -> numGamesAtomicInteger.getAndSet(numGames),
         (thisScoreboard, parsedScoreboard) -> {
            List<String> thisScoreboardRows = thisScoreboard.prepareScoreboard();
            List<String> parsedScoreboardRows = parsedScoreboard.prepareScoreboard();
            TestUtils.doAssertEqualRank(1, thisScoreboardRows, parsedScoreboardRows);
            TestUtils.doAssertScore(numGamesAtomicInteger.get()/5 , thisScoreboardRows, parsedScoreboardRows);
         });

      this.testValidityOfGeneratedInput(tmpl,
         TestUtils.newRoundRobinAllZeroes(3),
         numGames -> numGamesAtomicInteger.getAndSet(numGames),
         (thisScoreboard, parsedScoreboard) -> {
            List<String> thisScoreboardRows = thisScoreboard.prepareScoreboard();
            List<String> parsedScoreboardRows = parsedScoreboard.prepareScoreboard();
            TestUtils.doAssertEqualRank(1, thisScoreboardRows, parsedScoreboardRows);
            TestUtils.doAssertScore(numGamesAtomicInteger.get()/5 , thisScoreboardRows, parsedScoreboardRows);
         });
   }

   private void testValidityOfGeneratedInput(
      String tmpl,
      Function<Integer, Set<SpanProblem.TeamAndScoresPair>> generatorFunction,
      Consumer<Integer> numGamesConsumer,
      BiConsumer<SpanProblem.Scoreboard, SpanProblem.Scoreboard> scoreboardsConsumer
   ) {
      AtomicInteger numGames = new AtomicInteger(10);
      while(numGames.get() < 1000000) {
         System.out.println(String.format(tmpl, numGames.get()));
         numGamesConsumer.accept(numGames.get());
         this.testValidityOfGeneratedInput(numGames.get(),
            generatorFunction, scoreboardsConsumer);
         numGames.getAndSet(numGames.get() * 10);
      }

   }


   private void testValidityOfGeneratedInput(int numGames, Function<Integer, Set<SpanProblem.TeamAndScoresPair>> generatorFunction, BiConsumer<SpanProblem.Scoreboard, SpanProblem.Scoreboard> scoreboardsConsumer) {
      SpanProblem spanProblem = TestUtils.newSpanProblem();

      Set<SpanProblem.TeamAndScoresPair> teamAndScoresPairs = generatorFunction.apply(numGames);

      Set<SpanProblem.ScanResult> scanResults = spanProblem.handleInputStream(TestUtils.toInputStream.apply(teamAndScoresPairs));

      TestUtils.doAssertScanIsSuccess(scanResults.iterator());

      SpanProblem.ScoresAccumulator scoresAccumulator = new SpanProblem.ScoresAccumulator();

      teamAndScoresPairs.forEach(teamAndScoresPair -> scoresAccumulator.add(teamAndScoresPair));

      SpanProblem.Scoreboard thisScoreboard = scoresAccumulator.buildScoreboard();

      SpanProblem.Scoreboard parsedScoreboard = scanResults.iterator().next().asSuccess().getScoreboard();

      List<String> thisScoreboardRows = thisScoreboard.prepareScoreboard();

      List<String> parsedScoreboardRows = parsedScoreboard.prepareScoreboard();

      TestUtils.doAssertEquals(thisScoreboardRows, parsedScoreboardRows);

      System.out.println(String.format("Success: <%s>", "testValidityOfDefaultValidInput"));

      scoreboardsConsumer.accept(thisScoreboard, parsedScoreboard);
   }
}
