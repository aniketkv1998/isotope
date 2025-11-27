import streamlit as st
import pandas as pd
import os

def load_data():
    if os.path.exists("trades.csv"):
        return pd.read_csv("trades.csv")
    return pd.DataFrame(columns=["timestamp", "strategy_id", "symbol", "action", "quantity", "price"])

def main():
    st.set_page_config(page_title="Isotope Backtest Dashboard", layout="wide")
    st.title("Isotope Research & Backtesting Dashboard")

    df = load_data()

    if df.empty:
        st.warning("No trade data found in trades.csv. Run the backtest first.")
        return

    # Process Data
    try:
        df['timestamp'] = pd.to_datetime(df['timestamp'])
        df = df.sort_values('timestamp')

        # Calculate PnL
        # Assuming FIFO or simply realized PnL based on flat entry/exit assumptions is tricky without position tracking.
        # Simple Approach:
        # Cash Flow = -Price * Quantity (for BUY)
        # Cash Flow = +Price * Quantity (for SELL)
        # We will track a running cash balance.
        # But to show "Cumulative PnL", we need to account for position value.
        # However, without current market data, we can only show realized PnL or assuming trades close out.
        # Let's assume the backtest closes all positions at the end or we just track Cash Flow + (Current Position * Last Price).
        # But we don't have "Last Price" history here, only trade execution prices.

        # Alternative: Just calculate realized PnL for matching trades.
        # Given the complexity, let's implement a simple running PnL calculator based on closing out positions.
        # Or simpler: Cumulative Cash Flow. But that dips when buying.
        # Let's try to calculate Realized PnL.

        trades = df.to_dict('records')
        positions = {} # symbol -> {'quantity': 0, 'avg_price': 0}
        pnl_history = []
        cumulative_pnl = 0
        total_trades = 0
        winning_trades = 0

        for trade in trades:
            symbol = trade['symbol']
            action = trade['action']
            qty = trade['quantity']
            price = trade['price']
            timestamp = trade['timestamp']

            if symbol not in positions:
                positions[symbol] = {'quantity': 0, 'avg_price': 0}

            pos = positions[symbol]
            current_qty = pos['quantity']
            avg_price = pos['avg_price']

            trade_pnl = 0

            if action == 'BUY':
                # Increasing long position or closing short
                if current_qty < 0:
                    # Closing short
                    covered_qty = min(abs(current_qty), qty)
                    trade_pnl = (avg_price - price) * covered_qty
                    remaining_qty = qty - covered_qty

                    # Update position
                    new_qty = current_qty + covered_qty # moves towards 0
                    if new_qty == 0:
                        pos['quantity'] = 0
                        pos['avg_price'] = 0
                    else:
                         pos['quantity'] = new_qty

                    # If we flipped to long
                    if remaining_qty > 0:
                        pos['quantity'] = remaining_qty
                        pos['avg_price'] = price

                else:
                    # Adding to long
                    total_cost = (current_qty * avg_price) + (qty * price)
                    pos['quantity'] = current_qty + qty
                    pos['avg_price'] = total_cost / pos['quantity']

            elif action == 'SELL':
                # Closing long or opening short
                if current_qty > 0:
                    # Closing long
                    sold_qty = min(current_qty, qty)
                    trade_pnl = (price - avg_price) * sold_qty
                    remaining_qty = qty - sold_qty

                    # Update position
                    new_qty = current_qty - sold_qty
                    if new_qty == 0:
                        pos['quantity'] = 0
                        pos['avg_price'] = 0
                    else:
                        pos['quantity'] = new_qty

                    # If flipped to short
                    if remaining_qty > 0:
                        pos['quantity'] = -remaining_qty
                        pos['avg_price'] = price

                else:
                    # Adding to short
                    total_entry = (abs(current_qty) * avg_price) + (qty * price)
                    pos['quantity'] = current_qty - qty
                    pos['avg_price'] = total_entry / abs(pos['quantity'])

            if trade_pnl != 0:
                cumulative_pnl += trade_pnl
                total_trades += 1
                if trade_pnl > 0:
                    winning_trades += 1

            pnl_history.append({'timestamp': timestamp, 'cumulative_pnl': cumulative_pnl})

        pnl_df = pd.DataFrame(pnl_history)

        # Metrics
        total_profit = cumulative_pnl
        win_rate = (winning_trades / total_trades * 100) if total_trades > 0 else 0

        # Layout
        col1, col2, col3 = st.columns(3)
        col1.metric("Total Profit", f"{total_profit:,.2f}")
        col2.metric("Win Rate", f"{win_rate:.2f}%")
        col3.metric("Total Trades (Closed)", total_trades)

        # Chart
        st.subheader("Cumulative PnL Over Time")
        if not pnl_df.empty:
            st.line_chart(pnl_df.set_index('timestamp')['cumulative_pnl'])
        else:
            st.info("No closed trades to show PnL.")

        # Raw Data
        st.subheader("Raw Trade List")
        st.dataframe(df)

    except Exception as e:
        st.error(f"Error processing data: {e}")

if __name__ == "__main__":
    main()
