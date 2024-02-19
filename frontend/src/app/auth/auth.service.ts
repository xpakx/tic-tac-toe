import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { RegisterRequest } from './dto/register-request';
import { Observable } from 'rxjs';
import { AuthResponse } from './dto/auth-response';
import { AuthRequest } from './dto/auth-request';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl: String = "http://localhost:8000"

  constructor(protected http: HttpClient) { }

  public register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request);
  }

  public login(request: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/authenticate`, request);
  }
}