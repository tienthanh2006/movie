package com.cinema.venue.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Tạo hàng loạt sơ đồ ghế: hàng A, B, C... mỗi hàng có seatsPerRow ghế (A1, A2...)
public class GenerateSeatsRequest {

    @NotNull(message = "Số hàng không được để trống")
    @Min(value = 1, message = "Số hàng tối thiểu là 1")
    @Max(value = 26, message = "Số hàng tối đa là 26 (A-Z)")
    private Integer rows;

    @NotNull(message = "Số ghế mỗi hàng không được để trống")
    @Min(value = 1, message = "Số ghế mỗi hàng tối thiểu là 1")
    @Max(value = 30, message = "Số ghế mỗi hàng tối đa là 30")
    private Integer seatsPerRow;

    // Ví dụ ["D","E"]: các hàng này là ghế VIP
    private List<String> vipRows;

    // Ví dụ ["F"]: các hàng này là ghế đôi
    private List<String> sweetboxRows;

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }
    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }
    public List<String> getVipRows() { return vipRows; }
    public void setVipRows(List<String> vipRows) { this.vipRows = vipRows; }
    public List<String> getSweetboxRows() { return sweetboxRows; }
    public void setSweetboxRows(List<String> sweetboxRows) { this.sweetboxRows = sweetboxRows; }
}
