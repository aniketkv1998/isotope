import pandas as pd
import os

# Get the directory where this script is located
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# 1. Setup Paths (Download these files from Kaggle first)
NIFTY_PATH = os.path.join(SCRIPT_DIR, "NIFTY_50_minute.csv")      # Kaggle file name
BANKNIFTY_PATH = os.path.join(SCRIPT_DIR, "BANKNIFTY_minute.csv") # Kaggle file name
OUTPUT = os.path.join(SCRIPT_DIR, "../src/main/resources/market_data.csv")

def process_kaggle_data():
    print("Processing Kaggle Data...")

    # 2. Read Nifty
    # Kaggle format usually: "date,open,high,low,close,volume" (lowercase or uppercase)
    df_n = pd.read_csv(NIFTY_PATH)
    df_n['symbol'] = 'NIFTY'

    # 3. Read BankNifty
    df_b = pd.read_csv(BANKNIFTY_PATH)
    df_b['symbol'] = 'BANKNIFTY'

    # 4. Combine
    df = pd.concat([df_n, df_b])

    # 5. Standardize Column Names
    # Ensure columns map to: timestamp, symbol, open, high, low, close, volume
    # Note: Kaggle timestamps are often "2021-01-01 09:15:00".
    # Java needs ISO "2021-01-01T09:15:00".

    df.rename(columns={
        'date': 'timestamp', 'Date': 'timestamp', 'datetime': 'timestamp',
        'open': 'open', 'high': 'high', 'low': 'low', 'close': 'close', 'volume': 'volume'
    }, inplace=True)

    # Convert Timestamp to ISO format (Add 'T')
    print("Fixing Timestamps...")
    df['timestamp'] = pd.to_datetime(df['timestamp'])
    df['timestamp'] = df['timestamp'].dt.strftime('%Y-%m-%dT%H:%M:%S')

    # Filter for last 1 Year (Optional, if file is huge)
    # df = df[df['timestamp'] > "2023-01-01"]

    # 6. Select & Sort
    df_final = df[['timestamp', 'symbol', 'open', 'high', 'low', 'close', 'volume']]
    df_final = df_final.sort_values(by='timestamp')

    # 7. Save
    df_final.to_csv(OUTPUT, index=False)
    print(f"Done! Saved {len(df_final)} rows to {OUTPUT}")

if __name__ == "__main__":
    process_kaggle_data()