package org.nico.dbpool.nico;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

public class NicoDataSource extends NicoDataSourceWrapper implements DataSource{

    private String url;
    
    private String username;
    
    private String password;
    
    private String driver;
    
    private int init;
    
    private int max;
    
    private int min;
    
    private long waitTime;
    
    private NicoConnectionPool pool;
    
    private volatile boolean isInit;
    private volatile boolean inited;
    
    
    final Lock initLock = new ReentrantLock();
    final Condition initCondition = initLock.newCondition();
    
    @Override
    public Connection getConnection() throws SQLException {
        try {
            initLock.lock();
            init();
            if(! inited) {
                initCondition.await();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            initLock.unlock();
        }
        
        NicoConnection cn = null;
        if(pool.hasLeisure()) {
            cn = pool.getConnection(waitTime);
        }else if(! pool.isFull()) {
            cn = pool.addUsedConnection(createNativeConnection(pool));
        }else {
            cn = pool.getConnection(waitTime);
        }
        
        if(cn.isClosed()) {
            return pool.putUsedConnection(createNativeConnection(pool), cn.getId());
        }
        return cn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }
    
    private void init() throws ClassNotFoundException, SQLException {
        if(isInit) {
            return;
        }
        isInit = true;
        
        Class.forName(driver);
        
        pool = new NicoConnectionPool(max);
        
        for(int i = 0; i < init; i ++) {
            pool.addUnUsedConnection(createNativeConnection(pool));
        }
        inited = true;
        initCondition.signalAll();
    }
    
    public NicoConnectionPool getPool() {
        return pool;
    }

    public void setPool(NicoConnectionPool pool) {
        this.pool = pool;
    }

    public NicoConnection createNativeConnection(NicoConnectionPool pool) throws SQLException {
        return new NicoConnection(DriverManager.getConnection(url, username, password), pool);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    public int getInit() {
        return init;
    }

    public void setInit(int init) {
        this.init = init;
    }

}
