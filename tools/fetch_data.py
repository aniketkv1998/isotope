import yfinance as yf
import pandas as pd
import os

def fetch_and_save_data():
    symbols = {
        '^NSEI': 'NIFTY',
        '^NSEBANK': 'BANKNIFTY'
    }

    all_data = []

    print("Fetching data...")
    for ticker, name in symbols.items():
        try:
            # Fetch last 7 days of 1m data
            data = yf.download(ticker, period="7d", interval="1m", progress=False)

            if data.empty:
                print(f"Warning: No data found for {ticker}")
                continue

            # Reset index to make Datetime a column
            data = data.reset_index()

            # Ensure columns are flat (yfinance might return MultiIndex columns)
            if isinstance(data.columns, pd.MultiIndex):
                data.columns = data.columns.get_level_values(0)

            # Rename columns to match requirements (lowercase)
            data = data.rename(columns={
                'Datetime': 'timestamp',
                'Open': 'open',
                'High': 'high',
                'Low': 'low',
                'Close': 'close',
                'Volume': 'volume'
            })

            # Add symbol column
            data['symbol'] = name

            # Format timestamp to ISO format (YYYY-MM-DDTHH:MM:SS)
            # Remove timezone info for compatibility with Java LocalDateTime.parse
            data['timestamp'] = data['timestamp'].dt.tz_localize(None).dt.strftime('%Y-%m-%dT%H:%M:%S')

            # Select and reorder columns
            data = data[['timestamp', 'symbol', 'open', 'high', 'low', 'close', 'volume']]

            all_data.append(data)
            print(f"Fetched {len(data)} rows for {name}")

        except Exception as e:
            print(f"Error fetching data for {ticker}: {e}")

    if all_data:
        final_df = pd.concat(all_data)

        # Sort by timestamp
        final_df = final_df.sort_values('timestamp')

        # Define output path
        output_dir = os.path.join('src', 'main', 'resources')
        output_file = os.path.join(output_dir, 'market_data.csv')

        # Ensure directory exists
        os.makedirs(output_dir, exist_ok=True)

        # Save to CSV
        final_df.to_csv(output_file, index=False)
        print(f"Successfully saved {len(final_df)} rows to {output_file}")
    else:
        print("No data fetched.")

if __name__ == "__main__":
    fetch_and_save_data()
