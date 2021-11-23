package com.bobocode.dao;

import com.bobocode.exception.DaoOperationException;
import com.bobocode.model.Product;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;

@RequiredArgsConstructor
public class ProductDaoImpl implements ProductDao {
    private static final String INSERT_SQL = "insert into products (name, producer, price, expiration_date, creation_time) values(?,?,?,?,?)";
    private static final String UPDATE_SQL = "update products set name=?, producer=?, price=?, expiration_date=? where id = ?";
    private static final String FIND_ALL_SQL = "select * from products";
    private static final String FIND_ONE_SQL = "select * from products where id = ?";
    private static final String DELETE_PRODUCT_SQL = "delete products where id = ?";

    private final DataSource dataSource;

    @Override
    public void save(Product product) {
        doWithinConnection(conn -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS);
                stmt.setString(1, product.getName());
                stmt.setString(2, product.getProducer());
                stmt.setBigDecimal(3, product.getPrice());
                stmt.setDate(4, Date.valueOf(product.getExpirationDate()));
                stmt.setTimestamp(5, Timestamp.valueOf(now()));
                int rowsUpdated = stmt.executeUpdate();
                checkUpdated(rowsUpdated, () -> new DaoOperationException("Error saving product: " + product));
                product.setId(fetchGeneratedId(stmt));
            } catch (SQLException e) {
                throw new DaoOperationException("Error saving product: " + product, e);
            }
        });
    }

    @Override
    public List<Product> findAll() {
        return doWithinConnection(conn -> {
            try {
                Statement stmt = conn.createStatement();
                ResultSet resultSet = stmt.executeQuery(FIND_ALL_SQL);
                var products = new ArrayList<Product>();
                while (resultSet.next()) {
                    products.add(extractProduct(resultSet));
                }
                return products;
            } catch (SQLException e) {
                throw new DaoOperationException("Error getting all products", e);
            }
        });
    }

    @Override
    public Product findOne(Long id) {
        return doWithinConnection(conn -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(FIND_ONE_SQL);
                stmt.setLong(1, id);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    return extractProduct(resultSet);
                }
                throw new DaoOperationException(String.format("Product with id = %d does not exist", id));
            } catch (SQLException e) {
                throw new DaoOperationException("Error getting product", e);
            }
        });
    }

    @Override
    public void update(Product product) {
        if (product.getId() == null) throw new DaoOperationException("Product id cannot be null");
        doWithinConnection(conn -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL);
                stmt.setString(1, product.getName());
                stmt.setString(2, product.getProducer());
                stmt.setBigDecimal(3, product.getPrice());
                stmt.setDate(4, Date.valueOf(product.getExpirationDate()));
                stmt.setLong(5, product.getId());
                int rowsUpdated = stmt.executeUpdate();
                checkUpdated(rowsUpdated,
                        () -> new DaoOperationException(String.format("Product with id = %d does not exist", product.getId())));
            } catch (SQLException e) {
                throw new DaoOperationException("Error getting product", e);
            }
        });
    }

    @Override
    public void remove(Product product) {
        if (product.getId() == null) throw new DaoOperationException("Product id cannot be null");
        doWithinConnection(conn -> {
            try {
                PreparedStatement stmt = conn.prepareStatement(DELETE_PRODUCT_SQL);
                stmt.setLong(1, product.getId());
                int rowsUpdated = stmt.executeUpdate();
                checkUpdated(rowsUpdated, () -> new DaoOperationException(String.format("Product with id = %d does not exist", product.getId())));
            } catch (SQLException e) {
                throw new DaoOperationException("Error getting product", e);
            }
        });
    }

    private void doWithinConnection(Consumer<Connection> consumer) {
        try (var connection = dataSource.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            throw new DaoOperationException("Error", e);
        }
    }

    private <T> T doWithinConnection(Function<Connection, T> function) {
        try (var connection = dataSource.getConnection()) {
            return function.apply(connection);
        } catch (SQLException e) {
            throw new DaoOperationException("Error", e);
        }
    }

    private long fetchGeneratedId(PreparedStatement statement) throws SQLException {
        if (statement.getGeneratedKeys().next()) {
            return statement.getGeneratedKeys().getLong(1);
        } else {
            throw new DaoOperationException("Cannot fetch generated id");
        }
    }

    private Product extractProduct(ResultSet resultSet) throws SQLException {
        return Product.builder()
                .id(resultSet.getLong(1))
                .name(resultSet.getString(2))
                .producer(resultSet.getString(3))
                .price(resultSet.getBigDecimal(4))
                .expirationDate(resultSet.getDate(5).toLocalDate())
                .creationTime(resultSet.getTimestamp(6).toLocalDateTime())
                .build();
    }

    private <X extends Throwable> void checkUpdated(int rowsUpdated, Supplier<? extends X> exceptionSupplier) throws X {
        if (rowsUpdated == 0) {
            throw exceptionSupplier.get();
        }
    }
}
