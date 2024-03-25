package Services;

import Models.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    private final MovieService movieService = new MovieService();

    public List<Movie> getRecommendations(User user) {
        List<Movie> recommendedMovies = new ArrayList<>();
        List<Movie> recentlyViewed = user.getRecentlyViewed();

        if (recentlyViewed.size() < 2) {
            return getDefaultRecommendations();
        } else {
            // Original logic for recommendations based on user's recently viewed movies
            double averageRating = recentlyViewed.stream()
                    .mapToDouble(Movie::getRating)
                    .average()
                    .orElse(0.0);

            Map<String, Long> genreFrequency = recentlyViewed.stream()
                    .map(Movie::getGenre)
                    .flatMap(genre -> Arrays.stream(genre.split(", ")))
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()));

            List<String> mostCommonGenres = genreFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            IntSummaryStatistics releaseYearStats = recentlyViewed.stream()
                    .mapToInt(Movie::getReleaseYear)
                    .summaryStatistics();

            int minReleaseYear = releaseYearStats.getMin();
            int maxReleaseYear = releaseYearStats.getMax();

            String sql = "SELECT * FROM Movies WHERE rating > ? AND releaseYear BETWEEN ? AND ? " +
                    "AND (genre LIKE ? OR genre LIKE ?) " +
                    "LIMIT 6";

            try (Connection conn = DbFunctions.connect();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setDouble(1, averageRating);
                pstmt.setInt(2, minReleaseYear);
                pstmt.setInt(3, maxReleaseYear);
                pstmt.setString(4, "%" + mostCommonGenres.get(0) + "%");
                pstmt.setString(5, mostCommonGenres.size() > 1 ? "%" + mostCommonGenres.get(1) + "%"
                        : "%" + mostCommonGenres.get(0) + "%");

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Movie movie = movieService.resultSetToMovie(rs);
                    recommendedMovies.add(movie);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (recommendedMovies.isEmpty())
                return getDefaultRecommendations();
            else
                return recommendedMovies;
        }
    }

    public List<Movie> getDefaultRecommendations() {
        List<Movie> recommendedMovies = new ArrayList<>();
        String sql = "SELECT * FROM Movies ORDER BY rating DESC LIMIT 6";
        try (Connection conn = DbFunctions.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Movie movie = movieService.resultSetToMovie(rs);
                recommendedMovies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recommendedMovies;
    }

    // public List<Movie> getRecommendations(User user) {
    // List<Movie> recommendedMovies = new ArrayList<>();
    // List<Movie> recentlyViewed = user.getRecentlyViewed();
    //
    // if (recentlyViewed.isEmpty()) {
    // return getDefaultRecommendations();
    // } else {
    // // Dynamically adjust rating threshold based on user's viewing habits
    // double averageRating = recentlyViewed.stream()
    // .mapToDouble(Movie::getRating)
    // .average()
    // .orElse(0.0);
    //
    // double ratingStdDev = calculateStandardDeviation(recentlyViewed,
    // averageRating);
    // double lowerRatingThreshold = Math.max(averageRating - ratingStdDev, 0); //
    // Ensure threshold is not negative
    //
    // Set<String> uniqueGenres = recentlyViewed.stream()
    // .flatMap(movie -> movie.getGenres().stream()) // Assuming getGenres() returns
    // a List<String>
    // .collect(Collectors.toSet());
    //
    // // Fetch movies from the database based on preferences
    // recommendedMovies = fetchMoviesFromDatabase(lowerRatingThreshold,
    // uniqueGenres);
    //
    // if (recommendedMovies.isEmpty())
    // return getDefaultRecommendations();
    // else
    // for (Movie movie : recommendedMovies) {
    // System.out.println(movie.getTitle());
    // }
    // return recommendedMovies;
    // }
    // }
    //
    // private double calculateStandardDeviation(List<Movie> movies, double
    // averageRating) {
    // double sumSquaredDifferences = movies.stream()
    // .mapToDouble(movie -> Math.pow(movie.getRating() - averageRating, 2))
    // .sum();
    // return Math.sqrt(sumSquaredDifferences / movies.size());
    // }
    //
    // private List<Movie> fetchMoviesFromDatabase(double lowerRatingThreshold,
    // Set<String> uniqueGenres) {
    // List<Movie> movies = new ArrayList<>();
    // String sql = constructSqlQuery(lowerRatingThreshold, uniqueGenres);
    //
    // try (Connection conn = DbFunctions.connect();
    // PreparedStatement pstmt = conn.prepareStatement(sql)) {
    //
    // // Set parameters if using placeholders (?)
    // int index = 1;
    // pstmt.setDouble(index++, lowerRatingThreshold);
    // // Assuming genres are parameterized; otherwise, handle genre condition
    // construction safely
    //
    // ResultSet rs = pstmt.executeQuery();
    // while (rs.next()) {
    // Movie movie = movieService.resultSetToMovie(rs);
    // movies.add(movie);
    // }
    // } catch (SQLException e) {
    // e.printStackTrace();
    // }
    // return movies;
    // }
    //
    // private String constructSqlQuery(double lowerRatingThreshold, Set<String>
    // uniqueGenres) {
    // // Safe handling of dynamic parts; this example simplifies for brevity
    // String genreConditions = uniqueGenres.stream()
    // .map(genre -> "?")
    // .collect(Collectors.joining(", ")); // For use with IN clause, ensure safety
    //
    // return "SELECT * FROM Movies WHERE rating >= ? " +
    // "AND genre IN (" + genreConditions + ") " +
    // "ORDER BY rating DESC LIMIT 10";
    // // Note: Actual implementation must ensure SQL injection prevention
    // }

}
