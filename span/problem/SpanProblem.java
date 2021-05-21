package span.problem;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SpanProblem {

   static final Predicate<String[]> hasArgs = args -> 0 < args.length;

   static final Supplier<Set<ScanResult>> newResultsSet = () -> new HashSet();

   static final Function<String, Boolean> isValidRow = line -> {
      return true;
   };

   static final Function<String, String> normalise = line -> {
      return line.trim();
   };

   static final Function<String, TeamAndScoresPair> extractTeamAndScores = line -> {
      String[] segments = line.split(",");
      if(2 != segments.length){
         throw new RuntimeException();
      }
      TeamAndScore left = TeamAndScore.parseFrom(normalise.apply(segments[0]));
      TeamAndScore right = TeamAndScore.parseFrom(normalise.apply(segments[1]));
      return new TeamAndScoresPair(left, right);
   };

   public static void main(String[] args){
      SpanProblem spanProblem = new SpanProblem();
      if(hasArgs.test(args)){
         System.out.println(String.format("Not expecting any args - got <%s>", String.join(" ", args)));
         return;
      }
      spanProblem.handleStdIn();
   }

   private void handleStdIn(){
      Set<ScanResult> scanResults = this.handleInputStream(System.in);

      scanResults.iterator().next().asSuccess().getScoreboard().printScoreboard();
   }

   public Set<ScanResult> handleInputStream(InputStream inputStream){
      Set<ScanResult> scanResults = newResultsSet.get();
      Scanner scanner = new Scanner(inputStream);
      AtomicInteger counter = new AtomicInteger(0);

      ScoresAccumulator scoresAccumulator = new ScoresAccumulator();

      while(scanner.hasNext()){
         String line = scanner.nextLine();
         if(isValidRow.apply(line)){
           // System.out.println(String.format("%d: %s", counter.incrementAndGet(), line));
            TeamAndScoresPair teamAndScoresPair = extractTeamAndScores.apply(line);
            scoresAccumulator.add(teamAndScoresPair);
         }
      }
      Scoreboard scoreboard = scoresAccumulator.buildScoreboard();
      scanResults.add(new ScanResult.Success(scoreboard));
      return scanResults;
   }

   static class TeamAndScoresPair {
      final TeamAndScore left;
      final TeamAndScore right;

      TeamAndScoresPair(TeamAndScore left, TeamAndScore right) {
         this.left = left;
         this.right = right;
      }
   }

   static class Scoreboard {

      final Map<Team, AtomicInteger> scores = new HashMap<>();

      Scoreboard(Map<Team, AtomicInteger> scores) {
         this.scores.putAll(scores);
      }

      void printScoreboard() {
         List<String> scoreboardRows = prepareScoreboard();
         scoreboardRows.forEach(scoreboardRow -> System.out.println(scoreboardRow));
      }

      List<String> prepareScoreboard(){
         List<String> scoreboardRows = new ArrayList<>();
         Map<Integer, List<Team>> scoresToTeams = new HashMap<>();
         scores.keySet().forEach(team -> {
            int score = scores.get(team).get();
            //System.out.println(String.format("team <%s> score <%d>", team.name, score));
            if(!scoresToTeams.containsKey(score)){
               scoresToTeams.put(score, new ArrayList<>());
            }
            scoresToTeams.get(score).add(team);
         });

         List<Integer> orderedScores = new ArrayList(scoresToTeams.keySet());
         Collections.sort(orderedScores, Collections.reverseOrder());

         AtomicInteger rank = new AtomicInteger(0);

         orderedScores.forEach(score -> {
            List<String> teamsOfRank = new ArrayList<>();
            scoresToTeams.get(score).forEach(team -> teamsOfRank.add(team.name));
            Collections.sort(teamsOfRank);

            rank.incrementAndGet();

            teamsOfRank.forEach(teamOfRank -> {
               scoreboardRows.add(String.format("%d. %s, %d pts", rank.get(), teamOfRank, score));
            });
         });
         return scoreboardRows;
      }
   }

   static class ScoresAccumulator {

      Set<TeamAndScoresPair> teamAndScoresPairSet = new HashSet<>();
      Map<Team, AtomicInteger> scores = new HashMap<>();

      BiFunction<Integer, Integer, Integer> determinePoints = (a, b) -> {
         if(a == b){
            return 1;
         }
         if(a > b){
            return 3;
         }
         return 0;
      };

      Function<TeamAndScoresPair, Integer> getScoreForLeft = teamAndScoresPair -> determinePoints.apply(teamAndScoresPair.left.score.points, teamAndScoresPair.right.score.points);
      Function<TeamAndScoresPair, Integer> getScoreForRight = teamAndScoresPair -> determinePoints.apply(teamAndScoresPair.right.score.points, teamAndScoresPair.left.score.points);

      void add(TeamAndScoresPair teamAndScoresPair){
         teamAndScoresPairSet.add(teamAndScoresPair);
      }

      Scoreboard buildScoreboard(){
         teamAndScoresPairSet.forEach(teamAndScoresPair -> {
            if(!scores.containsKey(teamAndScoresPair.left.team)){
               scores.put(teamAndScoresPair.left.team, new AtomicInteger(0));
            }
            if(!scores.containsKey(teamAndScoresPair.right.team)){
               scores.put(teamAndScoresPair.right.team, new AtomicInteger(0));
            }

            int pointsForLeft = getScoreForLeft.apply(teamAndScoresPair);
            int pointsForRight = getScoreForRight.apply(teamAndScoresPair);

            scores.get(teamAndScoresPair.left.team).getAndAdd(pointsForLeft);
            scores.get(teamAndScoresPair.right.team).getAndAdd(pointsForRight);

         });
         return new Scoreboard(scores);
      }
   }

   static class TeamAndScore {
      final Team team;
      final Score score;

      TeamAndScore(Team team, Score score) {
         this.team = team;
         this.score = score;
      }

      void appendAsLiteral(StringBuilder stringBuilder){
         stringBuilder
            .append(this.team.name)
            .append(' ')
            .append(this.score.points)
         ;
      }

      boolean isTeam(TeamAndScore teamAndScore){
         return this.team.equals(teamAndScore.team);
      }

      static TeamAndScore parseFrom(String segment){
         String[] segments = segment.split(" ");
         if(2 > segments.length){
            throw new RuntimeException();
         }
         String points = segments[segments.length-1];
         String[] teamSegments = Arrays.copyOfRange(segments, 0, segments.length-1);
         return new TeamAndScore(
            new Team(String.join(" ", teamSegments)),
            new Score(Integer.parseInt(points))
         );
      }
   }

   static class Team {
      final String name;

      Team(String name) {
         this.name = name;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Team team = (Team) o;
         return Objects.equals(name, team.name);
      }

      @Override
      public int hashCode() {
         return Objects.hash(name);
      }
   }

   static class Score {
      final int points;

      Score(int points) {
         this.points = points;
      }
   }

   public interface ScanResult {

      default boolean isSuccess(){
         return this instanceof Success;
      }

      default Success asSuccess(){
         if(!isSuccess()){
            throw new IllegalArgumentException();
         }
         return (Success) this;
      }

      class Success implements ScanResult {
         private final Scoreboard scoreboard;

         public Success(Scoreboard scoreboard) {
            this.scoreboard = scoreboard;
         }

         public Scoreboard getScoreboard() {
            return scoreboard;
         }
      }
   }
}
