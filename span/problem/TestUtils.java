package span.problem;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class TestUtils {

   static final SpanProblem newSpanProblem() {
      return new SpanProblem();
   }

   static SpanProblem.Team barbarians = new SpanProblem.Team("Barbarians");
   static SpanProblem.Team crankyCretans = new SpanProblem.Team("Cranky Cretans");
   static SpanProblem.Team ionicIncredibles = new SpanProblem.Team("Ionic Incredibles");
   static SpanProblem.Team sparta = new SpanProblem.Team("Sparta");
   static SpanProblem.Team athens437BC = new SpanProblem.Team("Athens 437 BC");
   static SpanProblem.Team olympiaOlympic = new SpanProblem.Team("Olympia Olympic");
   static SpanProblem.Team boeotianBootleggers = new SpanProblem.Team("Boeotian Bootleggers");
   static SpanProblem.Team arcadianMagicians = new SpanProblem.Team("Arcadian Magicians");
   static SpanProblem.Team thebanThunder = new SpanProblem.Team("Theban Thunder");
   static SpanProblem.Team LegendaryLydians = new SpanProblem.Team("Legendary Lydians");

   static List<SpanProblem.Team> ALL_TEAMS = new ArrayList<>();

   static {
      ALL_TEAMS.add(barbarians);
      ALL_TEAMS.add(crankyCretans);
      ALL_TEAMS.add(sparta);
      ALL_TEAMS.add(athens437BC);
      ALL_TEAMS.add(olympiaOlympic);
      ALL_TEAMS.add(boeotianBootleggers);
      ALL_TEAMS.add(arcadianMagicians);
      ALL_TEAMS.add(thebanThunder);
      ALL_TEAMS.add(ionicIncredibles);
      ALL_TEAMS.add(LegendaryLydians);
   }

   private static final Function<Set<SpanProblem.TeamAndScoresPair>, Set<SpanProblem.TeamAndScoresPair>> ensureValid = teamAndScoresPairs -> {
      AtomicBoolean teamsEquals = new AtomicBoolean(true);
      teamAndScoresPairs.forEach(teamAndScoresPair -> {
         if (teamAndScoresPair.left.isTeam(teamAndScoresPair.right)) {
            teamsEquals.getAndSet(false);
         }
      });
      TestUtils.doAssert(teamsEquals.get(), String.format("Teams cannot play against themselves"));
      return teamAndScoresPairs;
   };

   static Function<Integer, Set<SpanProblem.TeamAndScoresPair>> randomAndRandom = (numGames) -> {
      Set<SpanProblem.TeamAndScoresPair> teamAndScoresPairs = new HashSet<>();
      AtomicInteger count = new AtomicInteger(numGames);
      int teamCount = ALL_TEAMS.size();
      while (0 < count.getAndDecrement()) {
         int leftIdx = (int) Math.floor(Math.random() * teamCount);
         int rightIdx = (int) Math.floor(Math.random() * teamCount);
         while (leftIdx == rightIdx) {
            rightIdx = (int) Math.floor(Math.random() * teamCount);
         }
         int leftPoints = (int) Math.round(6 * Math.random());
         int rightPoints = (int) Math.round(6 * Math.random());
         teamAndScoresPairs.add(
            new SpanProblem.TeamAndScoresPair(
               new SpanProblem.TeamAndScore(ALL_TEAMS.get(leftIdx), new SpanProblem.Score(leftPoints)),
               new SpanProblem.TeamAndScore(ALL_TEAMS.get(rightIdx), new SpanProblem.Score(rightPoints))
            )
         );
      }
      return ensureValid.apply(teamAndScoresPairs);
   };

   static class Roster {

      private final int teamCount;
      private final int roundsCount;

      private final AtomicReference<int[]> order;


      Roster(int teamCount) {
         this.teamCount = teamCount;
         this.roundsCount = 0 == teamCount % 2 ? teamCount / 2 : (teamCount - 1) / 2;
         this.order = new AtomicReference<>(new int[teamCount]);
         for (int i = 0; i < teamCount; i++) {
            this.order.get()[i] = i;
         }
      }

      int determineNext(int current) {
         int next = current + 1;
         if (next == teamCount) {
            return 1;
         }
         return next;
      }

      void march() {
         int[] nextOrder = Arrays.copyOf(order.get(), teamCount);
         for (int i = 0; i < teamCount; i++) {
            int next = i == 0 ? order.get()[i] : this.determineNext(order.get()[i]);
            //System.out.println(String.format("i <%d> this <%s> next <%s>", i, order.get()[i], next));
            nextOrder[i] = next;
         }
         this.order.getAndSet(nextOrder);
      }

      int getLeft(int round) {
         return order.get()[round * 2];
      }

      int getRight(int round) {
         return order.get()[(round * 2) + 1];
      }
   }

   static Function<Integer, Set<SpanProblem.TeamAndScoresPair>> newRoundRobinAllZeroes(int score) {
      return (numGames) -> {
         Set<SpanProblem.TeamAndScoresPair> teamAndScoresPairs = new HashSet<>();
         if (10 > numGames) {
            return teamAndScoresPairs;
         }
         AtomicInteger count = new AtomicInteger(0);
         int teamCount = ALL_TEAMS.size();

         Roster roster = new Roster(teamCount);

         while (count.get() < numGames) {
            int roundMod = count.get() % roster.roundsCount;

            int leftIdx = roster.getLeft(roundMod);
            int rightIdx = roster.getRight(roundMod);

            teamAndScoresPairs.add(
               new SpanProblem.TeamAndScoresPair(
                  new SpanProblem.TeamAndScore(ALL_TEAMS.get(leftIdx), new SpanProblem.Score(score)),
                  new SpanProblem.TeamAndScore(ALL_TEAMS.get(rightIdx), new SpanProblem.Score(score))
               )
            );
            if (roundMod == roster.roundsCount - 1) {
               roster.march();
            }
            count.getAndIncrement();
         }
         return ensureValid.apply(teamAndScoresPairs);
      };
   }

   static final Function<Set<SpanProblem.TeamAndScoresPair>, InputStream> toInputStream = (teamAndScoresPairs) -> {
      StringBuilder stringBuilder = new StringBuilder();
      teamAndScoresPairs.forEach(teamAndScoresPair -> {
         teamAndScoresPair.left.appendAsLiteral(stringBuilder);
         stringBuilder.append(',');
         stringBuilder.append(' ');
         teamAndScoresPair.right.appendAsLiteral(stringBuilder);
         stringBuilder.append('\n');
      });
      return new ByteArrayInputStream(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
   };

   static void doAssert(boolean outcome, String message) {
      if (!outcome) {
         throw new RuntimeException(
            String.format("AssertionError: <%s>", message));
      }
   }

   static void doAssertEquals(List<String> a, List<String> b) {
      TestUtils.doAssert(a.size() == b.size(),
         String.format("Scoreboards have different sizes"));

      Iterator<String> aIterator = a.iterator();
      Iterator<String> bIterator = a.iterator();
      List<Integer> nonCorrespondingRows = new ArrayList<>();
      AtomicInteger lineCounter = new AtomicInteger(0);
      while (aIterator.hasNext()) {
         String aLine = aIterator.next();
         String bLine = bIterator.next();
         if (!aLine.equals(bLine)) {
            nonCorrespondingRows.add(lineCounter.incrementAndGet());
         }
      }
      if (0 < nonCorrespondingRows.size()) {
         nonCorrespondingRows.forEach(nonCorrespondingRow -> {
            System.out.println(
               String.format("Non-corresponding row at line <%d>", nonCorrespondingRow));
         });
         throw new RuntimeException(
            String.format("Non-corresponding rows exist"));
      }
   }

   static int extractRank(String line) {
      String[] segments = line.split(" ");
      int rank = Integer.parseInt(segments[0].replace(".", ""));
      return rank;
   }

   static int extractScore(String line) {
      String[] segments = line.split(" ");
      int score = Integer.parseInt(segments[segments.length - 2]);
      return score;
   }

   static void doAssertEqualRank(int expectedRank, List<String> a, List<String> b) {
      TestUtils.doAssert(a.size() == b.size(),
         String.format("Scoreboards have different sizes"));

      Iterator<String> aIterator = a.iterator();
      Iterator<String> bIterator = a.iterator();

      AtomicInteger lineCounter = new AtomicInteger(0);
      while (aIterator.hasNext()) {
         String aLine = aIterator.next();
         String bLine = bIterator.next();

         int aRank = extractRank(aLine);
         int bRank = extractRank(bLine);

         TestUtils.doAssert(aRank == expectedRank,
            String.format("Expected rank <%d> got <%d>", expectedRank, aRank));

         TestUtils.doAssert(aRank == bRank,
            String.format("Ranks should be equal <%d> v <%d>", aRank, bRank));

         lineCounter.incrementAndGet();
      }
   }

   static void doAssertScore(int expectedScore, List<String> a, List<String> b) {
      TestUtils.doAssert(a.size() == b.size(),
         String.format("Scoreboards have different sizes"));

      Iterator<String> aIterator = a.iterator();
      Iterator<String> bIterator = a.iterator();

      AtomicInteger lineCounter = new AtomicInteger(0);
      while (aIterator.hasNext()) {
         String aLine = aIterator.next();
         String bLine = bIterator.next();

         int aScore = extractScore(aLine);
         int bScore = extractScore(bLine);

         TestUtils.doAssert(aScore == expectedScore,
            String.format("Expected rank <%d> got <%d>", expectedScore, aScore));

         TestUtils.doAssert(aScore == bScore,
            String.format("Scores should be equal <%d> v <%d>", aScore, bScore));

         lineCounter.incrementAndGet();
      }
   }

   static void doAssertScanIsSuccess(Iterator<SpanProblem.ScanResult> scanResultIterator) {
      TestUtils.doAssert(scanResultIterator.hasNext(),
         String.format("Expecting at least one value"));
      TestUtils.doAssert(scanResultIterator.next().isSuccess(),
         String.format("Expecting parse to indicate success"));
      TestUtils.doAssert(!scanResultIterator.hasNext(),
         String.format("Not expecting any more values"));
   }

   public static void main(String[] args) {
      if (1 != args.length) {
         System.out.println(String.format("Expecting an (integer) argument indicating how many rows to generate"));
         return;
      }
      AtomicInteger numGames = new AtomicInteger(0);
      if (1 == args.length) {
         try {
            int count = Integer.parseInt(args[0]);
            if (100000 < count) {
               System.out.println(String.format("Refusing to generate <%d> rows", count));
               return;
            }
            numGames.getAndSet(count);
         } catch (Throwable t) {
            System.out.println(String.format("Failed to convert argument <%s> to an integer", args[0]));
            return;
         }
      }

      Set<SpanProblem.TeamAndScoresPair> teamAndScoresPairs = TestUtils.randomAndRandom.apply(numGames.get());

      InputStream inputStream = TestUtils.toInputStream.apply(teamAndScoresPairs);

      String filePath = String.format("%s/%s", System.getProperty("java.io.tmpdir"), "data.csv");

      byte[] newlineBytes = "\n".getBytes(StandardCharsets.UTF_8);

      try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
         Scanner scanner = new Scanner(inputStream);
         while (scanner.hasNext()) {
            String line = scanner.nextLine();
            fileOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write(newlineBytes);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      System.out.println(String.format("Wrote test data to <%s>", filePath));
   }
}
