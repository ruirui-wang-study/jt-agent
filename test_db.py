import os
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
import oracledb

load_dotenv()

def test_connection():
    user = os.getenv("DB_USER")
    password = os.getenv("DB_PASSWORD")
    host = os.getenv("DB_HOST", "127.0.0.1")
    port = os.getenv("DB_PORT", "1521")
    service_name = os.getenv("DB_SERVICE_NAME", "helowin")
    
    print(f"Testing connection to: {host}:{port}/{service_name} as '{user}'")
    print(f"Password length: {len(password) if password else 0}")
    
    dsn = f"{host}:{port}/{service_name}"
    connection_string = f"oracle+oracledb://{user}:{password}@{dsn}"
    
    try:
        engine = create_engine(connection_string)
        with engine.connect() as connection:
            result = connection.execute(text("SELECT 1 FROM DUAL")).fetchone()
            print(f"Connection successful! Result: {result[0]}")
            
            # Try to query the table we are interested in
            try:
                print("Checking table YL_TMS_TEMP_CAR_TENDER...")
                result = connection.execute(text("SELECT count(*) FROM YL_TMS_TEMP_CAR_TENDER")).fetchone()
                print(f"Table found. Row count: {result[0]}")
            except Exception as table_e:
                print(f"Connected, but failed to query table: {table_e}")
                
    except Exception as e:
        print(f"Connection failed: {e}")

if __name__ == "__main__":
    test_connection()
