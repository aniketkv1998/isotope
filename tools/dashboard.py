import streamlit as st
import pandas as pd
import os

def load_data():
    if os.path.exists("trades.csv"):
        # The CSV now has additional columns: fees, net_cash_flow, running_balance
        return pd.read_csv("trades.csv")
    return pd.DataFrame(columns=["timestamp", "strategy_id", "symbol", "action", "quantity", "price", "fees", "net_cash_flow", "running_balance"])

def main():
    st.set_page_config(page_title="Isotope Backtest Dashboard", layout="wide")
    st.title("Isotope Research & Backtesting Dashboard")

    df = load_data()

    if df.empty:
        st.warning("No trade data found in trades.csv. Run the backtest first.")
        return

    # Process Data
    try:
        # Convert timestamp to datetime (assuming it is in milliseconds as per Java code)
        df['timestamp'] = pd.to_datetime(df['timestamp'], unit='ms')
        df = df.sort_values('timestamp')

        # Check if new columns exist
        if 'running_balance' not in df.columns:
            st.error("The 'trades.csv' file format is outdated. Please run the backtest again with the new engine.")
            st.dataframe(df.head())
            return

        # Metrics Calculation
        initial_capital = 0.0
        final_balance = 0.0

        # We can infer initial capital by reverse engineering the first row if needed,
        # but better to assume the first row's running balance is the result of the first trade.
        # running_balance_i = initial_capital + sum(net_cash_flow_0_to_i)
        # So initial_capital = running_balance_0 - net_cash_flow_0
        if not df.empty:
            first_row = df.iloc[0]
            initial_capital = first_row['running_balance'] - first_row['net_cash_flow']
            final_balance = df.iloc[-1]['running_balance']

        total_fees = df['fees'].sum()
        net_profit = final_balance - initial_capital
        net_profit_pct = (net_profit / initial_capital * 100) if initial_capital > 0 else 0.0

        # Layout Metrics
        col1, col2, col3, col4 = st.columns(4)
        col1.metric("Initial Capital", f"₹{initial_capital:,.2f}")
        col2.metric("Final Balance", f"₹{final_balance:,.2f}")
        col3.metric("Total Fees Paid", f"₹{total_fees:,.2f}")
        col4.metric("Net Profit %", f"{net_profit_pct:.2f}%")

        # Chart: Running Balance Over Time
        st.subheader("Account Balance Curve")
        st.line_chart(df.set_index('timestamp')['running_balance'])

        # Raw Data
        st.subheader("Trade Log")
        st.dataframe(df)

    except Exception as e:
        st.error(f"Error processing data: {e}")

if __name__ == "__main__":
    main()
