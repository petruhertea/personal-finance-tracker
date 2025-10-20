import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { UserResponse } from '../../common/user-response';

@Component({
  selector: 'app-profile',
  imports: [],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {

  currentUser: UserResponse | undefined;

  constructor(private authService: AuthService){}

  ngOnInit(): void {
    this.authService.getUser().subscribe({

      next: user => {
        if (!user) throw new Error('User not authenticated');
        this.currentUser = user

        console.log(this.currentUser);
      },
      error: e=> console.error(e)
    });
  }

}
